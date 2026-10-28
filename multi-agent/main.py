import json
import logging
from typing import Optional

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from infrastructure.providers import search_vectors, get_chroma_collection
from models.config import config
from models.schemas import (
    ChatRequest,
    ChatResponse,
    PaperGenerateRequest,
    QuestionRecommendRequest,
    StudentAnalysisRequest,
)
from orchestrator.workflow import (
    AgentState, Intent, multi_agent_workflow,
    run_collaboration, run_collaboration_streaming,
)

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger("multi-agent")

app = FastAPI(title="XZS Multi-Agent System", version="2.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ═══════════════════════════════════════════════════════════════
#  新增请求模型（内联定义，避免修改 schemas.py 的兼容性风险）
# ═══════════════════════════════════════════════════════════════

class KnowledgeSearchRequest(BaseModel):
    query: str
    top_k: int = Field(default=5, ge=1, le=50)
    collection: Optional[str] = None


class KnowledgeIndexRequest(BaseModel):
    doc_id: int
    content: str
    metadata: dict = Field(default_factory=dict)
    collection: Optional[str] = None


class ResourceGenerateRequest(BaseModel):
    topic: str
    agent_type: str = Field(default="document",
        description="document | mind_map | exercise | extended_reading | video | code_example")
    context: dict = Field(default_factory=dict)
    stream: bool = False


class ResourceMultiGenerateRequest(BaseModel):
    topic: str
    agent_types: list[str] = Field(default_factory=list,
        description="最多 6 个：document, mind_map, exercise, extended_reading, video, code_example")
    context: dict = Field(default_factory=dict)


class CollaborationRequest(BaseModel):
    topic: str
    context: dict = Field(default_factory=dict,
        description="可含 profile(学生画像)、note 等上下文")
    request_id: str = ""
    stream: bool = False
    learning_feedback: dict = Field(default_factory=dict,
        description="学习交互反馈（答题正确率等），用于动态迭代决策")
    ablation: dict = Field(default_factory=dict,
        description="消融实验开关（论文用）：no_rag / no_debate / self_refine / no_retry")


# ═══════════════════════════════════════════════════════════════
#  知识库向量检索（新增）
# ═══════════════════════════════════════════════════════════════

@app.post("/api/v1/knowledge/search")
async def knowledge_search(req: KnowledgeSearchRequest) -> dict:
    """
    ChromaDB 向量语义检索。
    由 Java KnowledgeBaseController 调用，替代关键词匹配。
    返回与查询语义最相似的文档片段。
    """
    logger.info("knowledge-search: query=%s top_k=%d", req.query[:80], req.top_k)
    try:
        results = search_vectors(req.query, top_k=req.top_k, collection=req.collection)
        return {"results": results}
    except Exception as e:
        logger.error("Knowledge search failed: %s", e)
        return {"results": [], "error": str(e)}


@app.post("/api/v1/knowledge/index")
async def knowledge_index(req: KnowledgeIndexRequest) -> dict:
    """
    文档向量化入库。
    Java 端上传/更新知识库文档后调用，将文档内容写入 ChromaDB。
    自动分块 → embedding → 存入向量数据库。
    """
    logger.info("knowledge-index: doc_id=%d len=%d", req.doc_id, len(req.content))
    try:
        col = get_chroma_collection(req.collection)
        if col is None:
            return {"success": False, "message": "ChromaDB 集合不存在，请先初始化知识库"}

        # 文档分块（每块 ~500 字，重叠 50 字）
        chunks = _chunk_text(req.content, chunk_size=500, overlap=50)
        if not chunks:
            return {"success": False, "message": "文档内容为空"}

        # 为每个块生成唯一 ID 和元数据
        ids = [f"doc_{req.doc_id}_chunk_{i}" for i in range(len(chunks))]
        metadatas = []
        for i in range(len(chunks)):
            meta = dict(req.metadata)
            meta["doc_id"] = req.doc_id
            meta["chunk_index"] = i
            meta["total_chunks"] = len(chunks)
            metadatas.append(meta)

        # 先删除旧数据（如果存在）
        try:
            existing = col.get(where={"doc_id": req.doc_id})
            if existing and existing.get("ids"):
                col.delete(ids=existing["ids"])
        except Exception:
            pass

        # 写入 ChromaDB
        col.add(documents=chunks, metadatas=metadatas, ids=ids)

        logger.info("knowledge-index: doc_id=%d indexed %d chunks", req.doc_id, len(chunks))
        return {"success": True, "message": f"已入库 {len(chunks)} 个文本块", "chunks": len(chunks)}

    except Exception as e:
        logger.error("Knowledge index failed: %s", e)
        return {"success": False, "message": str(e)}


# ═══════════════════════════════════════════════════════════════
#  资源生成 — Supervisor 统一编排（新增）
# ═══════════════════════════════════════════════════════════════

# Agent 类型到 workflow intent 的映射
AGENT_TO_INTENT = {
    "document": Intent.RESOURCE_GENERATE,
    "mind_map": Intent.RESOURCE_GENERATE,
    "exercise": Intent.RESOURCE_GENERATE,
    "extended_reading": Intent.RESOURCE_GENERATE,
    "video": Intent.RESOURCE_GENERATE,
    "code_example": Intent.RESOURCE_GENERATE,
}

AGENT_DISPLAY_NAMES = {
    "document": "Doc Agent",
    "mind_map": "MindMap Agent",
    "exercise": "Quiz Agent",
    "extended_reading": "Reading Agent",
    "video": "Video Agent",
    "code_example": "Code Agent",
    "operation_guide": "Guide Agent",
}


@app.post("/api/v1/resource/generate")
async def resource_generate(req: ResourceGenerateRequest) -> dict:
    """
    通过 Supervisor 编排单个 Agent 生成教学资源。
    Java ResourceGenerationController 优先调用此端点，
    不可用时回退到 Java 端直连 DeepSeek。
    """
    agent_name = AGENT_DISPLAY_NAMES.get(req.agent_type, req.agent_type)
    logger.info("resource-generate: topic=%s agent=%s", req.topic[:60], req.agent_type)

    context = dict(req.context)
    context["agent_type"] = req.agent_type
    context["topic"] = req.topic

    user_message = (
        f"请以{agent_name}的身份，为教学主题「{req.topic}」生成高质量的{agent_name}内容。\n"
        f"上下文：{json.dumps(context, ensure_ascii=False)}"
    )

    try:
        result = multi_agent_workflow.invoke(AgentState(
            intent=Intent.RESOURCE_GENERATE,
            user_message=user_message,
            context=context,
        ))
        return {
            "reply": result["final_reply"],
            "agent_type": req.agent_type,
            "intent": Intent(result["intent"]).value,
            "sub_agent_results": result.get("agent_results", []),
        }
    except Exception as e:
        logger.error("Resource generation failed: %s", e)
        return {"reply": f"生成失败：{e}", "error": str(e)}


@app.post("/api/v1/resource/multi-generate")
async def resource_multi_generate(req: ResourceMultiGenerateRequest) -> dict:
    """
    Supervisor 协调多个 Agent 批量生成教学资源。
    流程：Supervisor 意图分析 → 路由激活 Agent → 各 Agent 生成 → 汇总输出
    """
    agent_types = req.agent_types if req.agent_types else [
        "document", "mind_map", "exercise", "extended_reading", "video", "code_example"
    ]
    logger.info("resource-multi-generate: topic=%s agents=%s", req.topic[:60], agent_types)

    results = {}
    all_agent_results = []
    success_count = 0

    for agent_type in agent_types:
        agent_name = AGENT_DISPLAY_NAMES.get(agent_type, agent_type)
        context = dict(req.context)
        context["agent_type"] = agent_type
        context["topic"] = req.topic

        user_message = (
            f"请以{agent_name}的身份，为教学主题「{req.topic}」生成高质量的{agent_name}内容。\n"
            f"上下文：{json.dumps(context, ensure_ascii=False)}"
        )

        try:
            result = multi_agent_workflow.invoke(AgentState(
                intent=Intent.RESOURCE_GENERATE,
                user_message=user_message,
                context=context,
            ))
            results[agent_type] = result["final_reply"]
            all_agent_results.extend(result.get("agent_results", []))
            success_count += 1
        except Exception as e:
            logger.error("Agent %s generation failed: %s", agent_type, e)
            results[agent_type] = f"生成失败：{e}"

    # Supervisor 汇总
    summary_prompt = (
        f"你刚刚协调了 {len(agent_types)} 个教学 Agent 为「{req.topic}」生成了资源。\n"
        f"成功 {success_count}/{len(agent_types)} 个。\n"
        f"请生成一段简洁的汇总说明，列出各 Agent 的生成结果要点。"
    )

    try:
        summary_state = multi_agent_workflow.invoke(AgentState(
            intent=Intent.GENERAL_CHAT,
            user_message=summary_prompt,
        ))
        summary = summary_state.get("final_reply", "")
    except Exception:
        summary = f"多智能体协作完成：{success_count}/{len(agent_types)} 个 Agent 成功生成"

    return {
        "topic": req.topic,
        "success_count": success_count,
        "total_agents": len(agent_types),
        "summary": summary,
        "results": results,
        "agent_details": all_agent_results,
    }


# ═══════════════════════════════════════════════════════════════
#  多智能体深度协同工作流（Supervisor规划→共享态多Agent生成→质量回审→聚合）
#  替代"路由到单Agent"模式，体现真正的多智能体协作。
# ═══════════════════════════════════════════════════════════════

@app.post("/api/v1/collaboration/run")
async def collaboration_run(req: CollaborationRequest) -> dict:
    """
    同步运行多智能体协作工作流，返回完整结果 + 执行轨迹。
    完整流程（命中 PDF「分析-生成-校验-决策」协同闭环）：
      Supervisor 规划 → Diagnosis 学情诊断 → MindMap/Doc/Reading/Code/Quiz/Video 共享态协同生成
      → Debate 辩论与交叉验证（消除幻觉）→ Quality Review 质量审查 → Adapt 动态迭代决策 → 聚合。
    """
    if req.ablation:
        logger.info("collaboration-run: ablation=%s", req.ablation)
    logger.info("collaboration-run: topic=%s", req.topic[:60])
    try:
        ctx = dict(req.context or {})
        if req.ablation:
            ctx["ablation"] = req.ablation
        result = run_collaboration(req.topic, ctx, req.request_id)
        return {
            "topic": req.topic,
            "reply": result["reply"],
            "outputs": result["outputs"],
            "review": result["review"],
            "plan": result["plan"],
            "trace": result["trace"],
            "retry_count": result["retry_count"],
            "diagnosis": result.get("diagnosis", {}),
            "debate": result.get("debate", {}),
            "adapt": result.get("adapt", {}),
            "ablation": result.get("ablation", {}),
        }
    except Exception as e:
        logger.exception("collaboration run failed")
        return {"reply": f"协作生成失败：{e}", "error": str(e)}


@app.post("/api/v1/collaboration/stream")
async def collaboration_stream(req: CollaborationRequest):
    """
    SSE 流式推送多智能体协作的真实执行事件。
    每个事件为一个 JSON 对象，事件类型：
      plan_start / plan_end / diagnosis_start / diagnosis_end /
      agent_start / agent_end / debate_start / debate_end /
      review_start / review_end / adapt_start / adapt_end /
      retry / final / error
    最终以 data: [DONE] 结束。
    """
    logger.info("collaboration-stream: topic=%s", req.topic[:60])

    def event_generator():
        q = run_collaboration_streaming(
            req.topic, req.context, req.request_id, req.learning_feedback
        )
        try:
            while True:
                try:
                    kind, payload = q.get(timeout=600)
                except Exception:  # noqa: BLE001
                    yield f"data: {json.dumps({'type': 'error', 'message': '生成超时'}, ensure_ascii=False)}\n\n"
                    break
                if kind == "done":
                    break
                if kind == "event":
                    yield f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"
                elif kind == "final":
                    yield f"data: {json.dumps({'type': 'final', **payload}, ensure_ascii=False)}\n\n"
                elif kind == "error":
                    yield f"data: {json.dumps({'type': 'error', 'message': payload}, ensure_ascii=False)}\n\n"
        finally:
            yield "data: [DONE]\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",  # Nginx 关闭缓冲，确保实时推送
            "Connection": "keep-alive",
        },
    )


# ═══════════════════════════════════════════════════════════════
#  已有端点（保持不变）
# ═══════════════════════════════════════════════════════════════

@app.post("/api/v1/chat", response_model=ChatResponse)
async def chat(req: ChatRequest) -> ChatResponse:
    logger.info("chat: user=%s role=%s msg=%s", req.user_id, req.user_role, req.message[:80])
    result = multi_agent_workflow.invoke(AgentState(
        user_message=req.message,
        messages=req.messages,
        user_id=req.user_id,
        user_role=req.user_role,
        context=req.context,
    ))
    return ChatResponse(
        intent=Intent(result["intent"]).value,
        reply=result["final_reply"],
        metadata={"complexity": result["complexity"], "mode": result["mode"]},
        sub_agent_results=result.get("agent_results", []),
    )


@app.post("/api/v1/paper/generate")
async def paper(req: PaperGenerateRequest) -> dict:
    result = multi_agent_workflow.invoke(AgentState(
        intent=Intent.PAPER_GENERATE,
        user_message=f"智能组卷：{req.model_dump_json()}",
        user_id=req.user_id,
        context=req.model_dump(),
    ))
    return {"reply": result["final_reply"], "details": result.get("agent_results", [])}


@app.post("/api/v1/student/analysis")
async def student_analysis(req: StudentAnalysisRequest) -> dict:
    result = multi_agent_workflow.invoke(AgentState(
        intent=Intent.STUDENT_ANALYSIS,
        user_message=f"学情分析：{req.model_dump_json()}",
        user_id=req.user_id,
        context=req.model_dump(),
    ))
    return {"reply": result["final_reply"], "details": result.get("agent_results", [])}


@app.post("/api/v1/question/recommend")
async def question_recommend(req: QuestionRecommendRequest) -> dict:
    result = multi_agent_workflow.invoke(AgentState(
        intent=Intent.QUESTION_RECOMMEND,
        user_message=f"练习推荐：{req.model_dump_json()}",
        user_id=req.user_id,
        context=req.model_dump(),
    ))
    return {"reply": result["final_reply"], "details": result.get("agent_results", [])}


@app.websocket("/api/v1/qa/stream")
async def qa_stream(ws: WebSocket):
    await ws.accept()
    try:
        while True:
            data = json.loads(await ws.receive_text())
            result = multi_agent_workflow.invoke(AgentState(
                intent=Intent.QA,
                user_message=data.get("message", ""),
                user_id=data.get("user_id"),
                context=data.get("context", {}),
            ))
            await ws.send_text(json.dumps({
                "type": "reply",
                "content": result["final_reply"],
                "details": result.get("agent_results", []),
            }, ensure_ascii=False))
    except WebSocketDisconnect:
        pass


@app.get("/api/v1/health")
async def health() -> dict:
    return {
        "status": "ok",
        "version": "2.1.0",
        "agents": list(AGENT_DISPLAY_NAMES.keys()),
        "chromadb": _check_chromadb(),
    }


# ═══════════════════════════════════════════════════════════════
#  工具函数
# ═══════════════════════════════════════════════════════════════

def _chunk_text(text: str, chunk_size: int = 500, overlap: int = 50) -> list[str]:
    """将文本按字符数分块，块间有重叠。"""
    if not text or not text.strip():
        return []
    text = text.strip()
    chunks = []
    start = 0
    while start < len(text):
        end = start + chunk_size
        chunk = text[start:end]
        chunks.append(chunk)
        start = end - overlap
        if start >= len(text):
            break
        # 防止无限循环
        if start <= 0:
            start = end
    return chunks


def _check_chromadb() -> str:
    """检查 ChromaDB 连接状态。"""
    try:
        col = get_chroma_collection()
        if col is not None:
            count = col.count()
            return f"connected ({count} documents)"
        return "collection not found"
    except Exception as e:
        return f"error: {e}"


if __name__ == "__main__":
    import uvicorn

    cfg = config.server
    logger.info("Starting on %s:%s", cfg["host"], cfg["port"])
    uvicorn.run("main:app", host=cfg["host"], port=cfg["port"], reload=True)
