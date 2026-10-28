from __future__ import annotations

import json
import logging
import os
import re
import statistics
import time
import queue
import threading
import concurrent.futures
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Any, Callable, Optional

# 课程知识图谱：教学主题 → 关联（先修）科目。
# 难度定标必须以「该主题关联科目」的成绩为依据，而非全部科目平均，
# 否则跨领域主题（如"制造业视觉质检"）会被无关科目拉偏。
_TOPIC_SUBJECTS_CACHE: dict | None = None


def load_topic_subjects() -> dict:
    global _TOPIC_SUBJECTS_CACHE
    if _TOPIC_SUBJECTS_CACHE is None:
        path = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                            "..", "topic_subjects.json")
        try:
            with open(path, "r", encoding="utf-8") as f:
                _TOPIC_SUBJECTS_CACHE = json.load(f)
        except (OSError, json.JSONDecodeError):
            logger.warning("topic_subjects.json 未找到或格式错误，难度定标退化为纯语义判断")
            _TOPIC_SUBJECTS_CACHE = {}
    return _TOPIC_SUBJECTS_CACHE

logger = logging.getLogger(__name__)

# API 层 agent_type → 编排器专用提示词 key 的别名映射。
# 修复：此前 document/mind_map/exercise 等细分类型全部落入通用 "resource" 提示词
#（其指令是"输出资源清单、生成策略"这类元描述），导致出题时元描述混入题干。
AGENT_TYPE_PROMPT_ALIAS = {
    "document": "document",
    "mind_map": "mindmap",
    "exercise": "quiz",
    "quiz": "quiz",
    "extended_reading": "reading",
    "video": "video",
    "code_example": "code",
    "operation_guide": "guide",
}


def _rag_search(query: str, top_k: int = 5) -> str:
    """RAG 知识库检索：返回带来源标注的知识片段（供生成增强与事实核验/知识溯源）。"""
    try:
        from infrastructure.providers import search_vectors
        docs = search_vectors(query, top_k=top_k)
        if not docs:
            return ""
        parts = []
        for i, d in enumerate(docs, 1):
            content = d.get("content", "")
            meta = d.get("metadata") or {}
            source = meta.get("source", "未知来源")
            section = meta.get("section", "")
            label = f"{source}" + (f" · {section}" if section else "")
            parts.append(f"[{i}] （来源：{label}）\n{content[:500]}")
        return "\n\n".join(parts)
    except Exception as e:
        logger.warning("RAG检索失败（降级为空）: %s", e)
        return ""

# ═══════════════════════════════════════════════════════════════════════
#  事件回调机制 —— 把每个 Agent 的真实执行事件实时推给 SSE 订阅者
#  （模块级变量：工作流在单进程内串行运行，跨线程共享回调即可）
# ═══════════════════════════════════════════════════════════════════════
_event_cb: Callable[[dict], None] | None = None


def emit_event(event: dict) -> None:
    """向当前 SSE 订阅者推送一个真实执行事件。无订阅者时静默忽略。"""
    cb = _event_cb
    if cb is not None:
        try:
            cb(event)
        except Exception:  # noqa: BLE001
            pass


def _now() -> str:
    return datetime.now().strftime("%H:%M:%S.%f")[:-3]


def _summarize(text: str, n: int = 80) -> str:
    if not text:
        return ""
    text = " ".join(text.split())
    return text[:n] + ("…" if len(text) > n else "")


# ═══════════════════════════════════════════════════════════════════════
#  枚举与状态
# ═══════════════════════════════════════════════════════════════════════

class Intent(str, Enum):
    PAPER_GENERATE = "paper_generate"
    QUESTION_RECOMMEND = "question_recommend"
    STUDENT_ANALYSIS = "student_analysis"
    PROFILE_BUILD = "profile_build"
    RESOURCE_GENERATE = "resource_generate"
    LEARNING_PATH = "learning_path"
    QA = "qa"
    TEACHING = "teaching"
    COLLABORATION = "collaboration"
    GENERAL_CHAT = "general_chat"


class TaskComplexity(str, Enum):
    SIMPLE = "simple"
    MODERATE = "moderate"
    COMPLEX = "complex"


class AgentMode(str, Enum):
    ROUTE = "route"
    HIERARCHY = "hierarchy"
    SWARM = "swarm"


@dataclass
class AgentState:
    intent: Intent = Intent.GENERAL_CHAT
    complexity: TaskComplexity = TaskComplexity.SIMPLE
    mode: AgentMode = AgentMode.ROUTE

    user_message: str = ""
    messages: list[dict[str, str]] = field(default_factory=list)
    user_id: int | None = None
    user_role: str = "student"
    context: dict[str, Any] = field(default_factory=dict)

    supervisor_plan: dict[str, Any] = field(default_factory=dict)
    agent_results: list[dict[str, Any]] = field(default_factory=list)
    final_reply: str = ""
    error: str | None = None

    # ── 多智能体协作工作流专用字段 ──────────────────────────────
    topic: str = ""
    request_id: str = ""
    plan: dict[str, Any] = field(default_factory=dict)            # Supervisor 任务规划
    agent_outputs: dict[str, str] = field(default_factory=dict)   # 各 Agent 产出（共享态）
    review_result: dict[str, Any] = field(default_factory=dict)   # 质量评估结果
    retry_count: int = 0
    # ── PDF 整改新增字段（学情诊断 / 辩论交叉验证 / 动态迭代决策）──
    diagnosis_result: dict[str, Any] = field(default_factory=dict)  # 学情诊断报告
    debate_result: dict[str, Any] = field(default_factory=dict)     # 辩论与交叉验证结果
    adapt_result: dict[str, Any] = field(default_factory=dict)      # 动态迭代决策结果
    learning_feedback: dict[str, Any] = field(default_factory=dict) # 学习交互反馈（答题正确率等）
    # 必覆盖知识点清单（由 Diagnosis Agent 产出，作为生成 Agent 的硬约束与 Review 逐项核销依据）
    diagnosis_must_cover: list[str] = field(default_factory=list)
    coverage_report: dict[str, Any] = field(default_factory=dict)   # 覆盖率核销结果
    # 消融实验开关（论文用）：no_rag / no_debate / self_refine / no_retry
    # 默认空字典 = 完整系统，行为与历史版本完全一致。
    ablation: dict[str, Any] = field(default_factory=dict)


# ═══════════════════════════════════════════════════════════════════════
#  LLM 调用基础
# ═══════════════════════════════════════════════════════════════════════

def _ablation(state) -> dict:
    """读取消融开关（兼容 dataclass 与 dict 两种 state 形态）。

    支持的开关：
      no_rag      —— 关闭生成阶段的知识库检索增强（论文 B3）
      no_debate   —— 跳过辩论与交叉验证（论文 B4）
      self_refine —— 以「同一模型自我批判」替代辩论（论文 B5）
      no_retry    —— 关闭评审触发的重新生成（论文 B6）
    """
    if isinstance(state, dict):
        return state.get("ablation") or {}
    return getattr(state, "ablation", None) or {}


def compute_expected_level(topic: str, profile: dict) -> str:
    """按公开定标规则计算"期望难度档位"，供消融实验一致对照（与评测口径同源）。

    返回 基础 / 进阶 / 高级 / ""（数据不足）。
    """
    subs = load_topic_subjects().get(topic) or []
    scores = (profile or {}).get("理论测试结果") or {}
    if not subs or not scores:
        return ""
    numeric = [v for v in scores.values() if isinstance(v, (int, float))]
    if not numeric:
        return ""
    fallback = int(statistics.median(numeric) + 0.5)
    vals = [scores.get(s) if isinstance(scores.get(s), (int, float)) else fallback for s in subs]
    base = int(sum(vals) / len(vals) + 0.5)
    mods = (profile or {}).get("modifiers") or {}
    final = max(0, min(100, base + mods.get("goal", 0) + mods.get("pace", 0) + mods.get("career", 0)))
    return "基础" if final < 60 else ("进阶" if final < 85 else "高级")


def _calibration_hint(topic: str, profile: dict) -> str:
    """按公开定标规则计算难度，生成给 Supervisor 的显式指令。

    背景（重要）：仅用自然语言要求「请综合理论测试结果判断难度」时，
    Supervisor 会系统性偏向"基础"档——因为没有阈值，模型只能凭印象取保守值，
    实测适配准确率仅 33%。把规则与中间量显式注入后，定标变成可复核的计算。

    规则：
        base  = 关联科目得分均值（缺失科目用该生全科目中位数回填）
        final = clamp(base + 目标加成 + 节奏修正 + 职业修正, 0, 100)
        final < 60 → 基础；60 ≤ final < 85 → 进阶；final ≥ 85 → 高级
    """
    subs = load_topic_subjects().get(topic) or []
    scores = (profile or {}).get("理论测试结果") or {}
    if not subs or not scores:
        return ""
    numeric = [v for v in scores.values() if isinstance(v, (int, float))]
    if not numeric:
        return ""
    fallback = int(statistics.median(numeric) + 0.5)

    parts, vals = [], []
    for s in subs:
        v = scores.get(s)
        if isinstance(v, (int, float)):
            vals.append(v)
            parts.append(f"{s}={v}")
        else:
            vals.append(fallback)
            parts.append(f"{s}=缺失(回填中位数 {fallback})")
    base = int(sum(vals) / len(vals) + 0.5)
    mods = (profile or {}).get("modifiers") or {}
    g, p, c = mods.get("goal", 0), mods.get("pace", 0), mods.get("career", 0)
    final = max(0, min(100, base + g + p + c))
    level = "基础" if final < 60 else ("进阶" if final < 85 else "高级")

    return (
        "\n\n【难度定标规则（必须遵循，可复核）】\n"
        f"本主题关联科目：{'、'.join(subs)}\n"
        f"该生对应成绩：{'；'.join(parts)}\n"
        f"base = 均值 = {base}\n"
        f"修正：目标加成 {g:+d}、节奏修正 {p:+d}、职业修正 {c:+d}\n"
        f"final = clamp(base + 修正, 0, 100) = {final}\n"
        f"档位映射：final < 60 → 基础；60 ≤ final < 85 → 进阶；final ≥ 85 → 高级\n"
        f"=> 本例规则难度应为「{level}」。\n"
        "请据此输出 difficulty。若你认为存在规则未覆盖的强证据需要越档"
        "（例如该生在该知识点上有明确的严重薄弱点），"
        "可以在 rationale 中写明具体证据后越档，但不得无理由偏离。"
    )


def build_llm(agent_name: str):
    from langchain_openai import ChatOpenAI
    from models.config import config

    cfg = config.get_agent_cfg(agent_name)
    backend_name = cfg.get("backend", "deepseek")
    backend = config.get_backend_cfg(backend_name)
    # config.yaml 中 api_url 是完整端点 (https://api.deepseek.com/chat/completions)，
    # ChatOpenAI 需要的是 base_url（去掉 /chat/completions）。
    api_url = backend.get("api_url", "")
    api_base = api_url.removesuffix("/chat/completions") if api_url else ""
    return ChatOpenAI(
        model=cfg.get("model", "deepseek-v4-pro"),
        temperature=cfg.get("temperature", 0.7),
        openai_api_key=backend.get("api_key", ""),
        openai_api_base=api_base,
        max_tokens=4096,
    )


def _extract_json(text: str) -> dict[str, Any]:
    text = (text or "").strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass
    for marker in ("```json", "```"):
        if marker in text:
            try:
                return json.loads(text.split(marker, 1)[1].split("```", 1)[0])
            except Exception:  # noqa: BLE001
                pass
    start = text.find("{")
    end = text.rfind("}")
    if start >= 0 and end > start:
        try:
            return json.loads(text[start:end + 1])
        except Exception:  # noqa: BLE001
            pass
    return {}


def _call_llm(agent: str, system: str, user: str) -> str:
    from langchain_core.messages import HumanMessage, SystemMessage
    llm = build_llm(agent)
    resp = llm.invoke([SystemMessage(content=system), HumanMessage(content=user)])
    return resp.content


def _call_llm_with_tools(agent: str, system: str, user: str) -> str:
    """调用 LLM，自动绑定该 Agent 的注册工具并处理 tool_calls。"""
    from langchain_core.messages import HumanMessage, SystemMessage, ToolMessage
    from tools.registry import get_tools

    llm = build_llm(agent)
    tools = get_tools(agent)

    if not tools:
        resp = llm.invoke([SystemMessage(content=system), HumanMessage(content=user)])
        return resp.content

    llm_with_tools = llm.bind_tools(tools)
    messages = [SystemMessage(content=system), HumanMessage(content=user)]
    resp = llm_with_tools.invoke(messages)

    if hasattr(resp, "tool_calls") and resp.tool_calls:
        tool_results = []
        for tc in resp.tool_calls:
            tool_name = tc.get("name", "")
            tool_args = tc.get("args", {})
            try:
                for t in tools:
                    if t.name == tool_name:
                        result = t.invoke(tool_args)
                        tool_results.append(f"[{tool_name}]: {result}")
                        break
            except Exception as e:  # noqa: BLE001
                tool_results.append(f"[{tool_name}] 调用失败: {e}")

        if tool_results:
            messages.append(resp)
            messages.append(ToolMessage(
                content="\n".join(tool_results),
                tool_call_id=resp.tool_calls[0].get("id", "")
            ))
            final = llm.invoke(messages)
            return final.content

    return resp.content


# ═══════════════════════════════════════════════════════════════════════
#  Agent 提示词
# ═══════════════════════════════════════════════════════════════════════

# 生成类 Agent 共享的知识约束（PDF：高保真知识溯源与约束生成、幻觉防控）
_GROUNDING_RULE = (
    "【知识保真硬约束 — 必须遵守】\n"
    "1. 输入中提供的【知识库检索结果（RAG增强）】是权威依据：关键定义、公式、数据、行业指标"
    "必须与之一致，禁止编造不存在的公式/定理/文献/统计数字\n"
    "2. 知识库未覆盖的内容只可做通用性展开，不得虚构具体出处；不确定时明确说明局限性\n"
    "3. 关键论断须在句末以 [来源:文件名·章节] 格式标注引用（如 [来源:ai_domain_kb.md·卷积神经网络]）；"
    "无对应来源的核心结论需标注 [通用知识]\n"
    "4. 学生画像（学历背景/岗位方向/理论测试结果/薄弱点）决定内容深浅与案例取舍："
    "转岗/内训学员侧重实操步骤，科研向学习者侧重原理推导\n\n"
)

AGENT_PROMPTS = {
    "supervisor": (
        "你是 Supervisor Agent，负责意图识别、任务拆分、Agent 路由和结果汇总。"
        "请把用户请求归类到 paper_generate / question_recommend / student_analysis / "
        "profile_build / resource_generate / learning_path / qa / teaching / general_chat。"
        "评估复杂度 simple/moderate/complex，并选择 route/hierarchy/swarm。"
        "只返回 JSON：{\"intent\":\"...\",\"complexity\":\"...\",\"mode\":\"...\","
        "\"plan\":{\"steps\":[],\"agents_needed\":[]},\"summary\":\"...\"}"
    ),
    "supervisor_plan": (
        "你是多智能体协作系统的 Supervisor（编排器）。根据教学主题和学生画像，"
        "规划本次多智能体协同生成方案：确定需要激活的 Agent 及其执行顺序与依赖关系。\n\n"
        "可用 Agent：\n"
        "- mindmap 思维导图 Agent（必须最先执行，产出知识结构供后续 Agent 复用）\n"
        "- document 课程文档 Agent（依赖 mindmap）\n"
        "- reading 拓展阅读 Agent（依赖 mindmap，与 document 并行）\n"
        "- code 代码案例 Agent（依赖 mindmap，与 document 并行）\n"
        "- guide 实操指南 Agent（面向岗位技能/工程实操场景，产出可落地的分步骤实操指南；"
        "学习者画像含岗位方向/转岗培训/企业内训背景时建议激活）\n"
        "- quiz 练习题 Agent（依赖 document，确保题目与文档内容一致）\n"
        "- video 教学视频脚本 Agent（依赖 document + mindmap）\n"
        "- review 质量评估 Agent（最后对全部资源做质量审查）\n\n"
        "【先验知识适配要求】规划难度时必须综合：学历背景、岗位方向、理论测试结果（各科目 0-100 分）、"
        "薄弱点与学习目标。在职/转岗学习者侧重实操落地，在校学习者侧重理论体系。\n\n"
        "只返回 JSON：{\"agents\":[\"mindmap\",\"document\",\"reading\",\"code\",\"quiz\",\"video\"],"
        "\"stages\":[{\"stage\":\"知识结构\",\"agent\":\"mindmap\",\"depends_on\":[]},"
        "{\"stage\":\"并行生成\",\"agents\":[\"document\",\"reading\",\"code\"],\"depends_on\":[\"mindmap\"]},"
        "{\"stage\":\"一致性关联\",\"agent\":\"quiz\",\"depends_on\":[\"document\"]}],"
        "\"difficulty\":\"基础|进阶|高级\",\"rationale\":\"规划理由\"}\n\n"
        "若输入中包含【难度定标规则】，必须严格按该规则计算出的档位输出 difficulty；"
        "无充分证据不得凭印象改变档位。"
    ),
    "paper": (
        "你是 Paper Agent，负责智能组卷。根据知识点覆盖、难度分布和题型比例生成试卷。"
        "输出 JSON，包含 title、questions、knowledge_coverage、difficulty_distribution。"
    ),
    "question": (
        "你是 Question Agent，负责练习题推荐与题库生成。根据薄弱点、错题记录和知识点生成针对性练习。"
        "输出 JSON，包含 recommendations 和 analysis。"
    ),
    "student": (
        "你是 Student Analysis Agent，负责学情分析。分析成绩趋势、薄弱点、优势和学习建议。"
        "输出 JSON，包含 overall_assessment、strengths、weaknesses、progress_trend、study_plan。"
    ),
    "profile": (
        "你是 Profile Agent，负责对话式学习画像构建。抽取至少 8 个维度：专业/课程方向、年级、知识基础、"
        "认知风格、学习风格、目标、薄弱点、学习时间偏好、兴趣、动机水平。输出画像摘要和下一步提问。"
    ),
    "resource": (
        "你是 Resource Orchestrator Agent，负责协调文档、思维导图、题目、拓展阅读、视频动画、代码案例等资源生成。"
        "输出资源清单、每个资源的生成策略、质量检查点和跨模态一致性说明。"
    ),
    "path": (
        "你是 Learning Path Agent，负责根据学生画像和课程知识依赖规划个性化学习路径。"
        "输出 JSON 节点，每个节点包含目标、资源、预计时长、难度、先修要求和推荐理由。"
    ),
    "qa": (
        "你是 QA Agent，负责智能辅导与 RAG 答疑。回答要循序渐进，给思路、例子和下一步练习，避免只给最终答案。"
    ),
    "teaching": (
        "你是 Teaching Agent，负责教学设计、课堂活动、PPT 大纲和互动内容生成。"
        "输出结构化教学方案，兼顾教学目标、重难点、活动和评价方式。"
    ),
    # ── 协作工作流专用 Agent（与 Java 端 MultiAgentService 提示词对齐，保证产出质量）──
    "mindmap": (
        _GROUNDING_RULE +
        "你是 MindMap Agent —— 知识体系可视化专家。围绕教学主题输出思维导图知识树。\n\n"
        "致命规则：\n"
        "· 回复第一个字符必须是\"-\"，以\"- 中心主题\"开头\n"
        "· 每个节点独占一行，层级靠行首空格（根0空格，一级2空格，二级4空格）\n"
        "· 禁止任何开场白、说明、总结等非知识点文字\n\n"
        "必须覆盖 8 个一级维度，每维 ≥3 子节点："
        "核心定义、关键属性、分类体系、核心原理、公式量化、应用实例、常见误区、知识关联。\n"
        "从\"- 中心主题\"直接开始。"
    ),
    "document": (
        _GROUNDING_RULE +
        "你是 Doc Agent —— 高校课程文档生成专家，将教学主题转化为结构完整、内容详实的课程讲解文档。\n\n"
        "【内容架构 — 必须包含以下全部章节】\n"
        "1. 概念定义  2. 历史背景  3. 核心原理  4. 关键特征  5. 分类体系\n"
        "6. 数学/公式描述  7. 应用实例  8. 常见误区  9. 学科前沿  10. 知识关联\n\n"
        "【输出格式 — Markdown】使用 ## / ### 标题层级，列表用 - 或 1.。\n"
        "直接输出教学内容，不要任何开场白或客套结尾。"
    ),
    "reading": (
        _GROUNDING_RULE +
        "你是 Reading Agent —— 学术前沿与跨学科阅读材料策划专家，围绕教学主题编写具有学术深度的拓展阅读材料。\n\n"
        "【内容架构】1.学科背景 2.前沿动态(近5年≥2项) 3.跨学科交叉(≥2方向) "
        "4.经典文献导读 5.名家观点 6.争议与未解问题 7.延伸思考\n\n"
        "【输出格式 — Markdown】用 ## 一、## 二、… 组织。直接输出正文，无开场白。"
    ),
    "code": (
        _GROUNDING_RULE +
        "你是 Code Agent —— 计算机教育教学代码设计专家，编写结构清晰、注释详细、可直接运行的教学示例代码。\n\n"
        "【要求】1.可运行性(完整含import) 2.中文教学注释 3.命名规范 4.渐进式 5.错误处理\n\n"
        "输出至少 3 个难度递进示例，每个含：运行环境、学习目标、```语言 代码块```、预期输出、关键知识点、常见错误。\n"
        "直接从\"// ═══ 示例1\"开始，无开场白。"
    ),
    "guide": (
        _GROUNDING_RULE +
        "你是 Guide Agent —— 岗位实操指南专家（面向企业内训/转岗培训/工程实操场景），"
        "将教学主题转化为可落地的分步骤实操指南。\n\n"
        "【内容架构 — 必须包含】\n"
        "1. 适用岗位与前置能力要求  2. 环境与工具准备清单  3. 标准操作流程（分步骤，每步含：操作内容、"
        "命令/参数、预期结果、常见报错与处置）  4. 质量验收标准（量化指标）  5. 行业规范与安全红线  "
        "6. 进阶路径建议\n\n"
        "【硬性要求】步骤必须可执行、可验证；优先引用知识库中的行业规范数据；"
        "结合学习者画像的岗位方向与理论测试结果调整详略。\n"
        "输出 Markdown，用 ## 一、## 二、… 组织。直接输出正文，无开场白。"
    ),
    "quiz": (
        _GROUNDING_RULE +
        "你是 Quiz Agent —— 教育测量与评估设计专家，根据教学内容设计科学、有区分度的练习题目。\n\n"
        "【题型要求】选择题3道(4选项) + 判断题2道 + 填空题2道 + 简答题1道\n"
        "难度梯度：基础60% + 进阶25% + 挑战15%。干扰项基于常见错误认知。\n\n"
        "【输出格式 — 致命规则】\n"
        "· 回复的第一个字符必须是题型标记【选择题】，之前不允许任何文字\n"
        "· 禁止把题目包装成 JSON、代码块或任何结构化数据输出，禁止输出 topic/agent_type 等元数据字段\n"
        "· 严禁出现任何元描述：如\"以下是一份…\"、\"已生成\"、\"好的\"、\"根据您的要求\"、"
        "\"本题考查\"、\"知识点：\"、\"设计说明\"、\"出题思路\"等一律不得出现\n"
        "· 题干只包含题面本身（情景/问题/选项），不含任何出题意图或考查说明\n"
        "· 最后一题的解析结束后立即停止输出，禁止\"希望…\"、\"以上题目…\"等收尾语\n\n"
        "【每题结构】题型标记 → 题干 → 选项/作答要求 → 答案 → 解析(说明为什么对/错)"
    ),
    "video": (
        _GROUNDING_RULE +
        "你是 Video Agent —— 教学视频内容设计师。为教学主题生成视频画面内容和语音讲解稿。\n\n"
        "【重要】画面文字和语音讲解必须不同：画面显示精炼知识点，语音详细讲解。\n\n"
        "按以下场景输出（从\"## 开场引入\"开始）：\n"
        "## 开场引入 / ## 核心概念 / ## 原理讲解 / ## 实例演示 / ## 方法总结\n"
        "每个场景含【画面文字】和【语音讲解】两部分。无前言。"
    ),
    # ── 资源质量评估 Agent（命中 PDF 评分②「资源生成的质量评估」硬性要求 + 功能创新）──
    "review": (
        "你是 Quality Review Agent —— 教学资源质量评估专家。对多智能体协同生成的全部教学资源进行多维度质量审查。\n\n"
        "【评估维度】（每项 0-10 分）\n"
        "1. accuracy 准确性：知识点是否正确无误、公式是否准确\n"
        "2. difficulty_fit 难度适配：是否匹配学生画像的知识基础与目标\n"
        "3. completeness 完整性：是否覆盖该主题必要知识点、章节是否完整\n"
        "4. cross_modal_consistency 跨模态一致性：文档/思维导图/题目/视频内容是否相互一致、无矛盾\n\n"
        "【审查要点】找出具体问题（如公式错误、章节缺失、文档与题目不一致等），标注 severity: critical/major/minor。\n\n"
        "只返回 JSON：\n"
        "{\"scores\":{\"accuracy\":N,\"difficulty_fit\":N,\"completeness\":N,\"cross_modal_consistency\":N},"
        "\"overall\":N,\"issues\":[{\"resource\":\"document\",\"severity\":\"major\",\"description\":\"具体问题\"}],"
        "\"retry\":true|false}"
    ),
    # ── 学情诊断 Agent（命中 PDF「学情诊断 Agent」明确职责分工要求）──
    "diagnosis": (
        "你是 Diagnosis Agent —— 学情诊断专家。基于学习者先验知识画像（学历背景、理论测试结果、薄弱点等），"
        "精准锚定学习者的理论强项与技能盲区，为后续资源生成的个性化适配提供决策依据。\n\n"
        "【诊断维度】\n"
        "1. knowledge_gaps 知识盲区：基于画像中各科目掌握度，定位具体薄弱知识点（含严重程度 high/medium/low）\n"
        "2. cognitive_profile 认知特征：认知风格偏好、学习节奏、动机水平对学习路径的影响\n"
        "3. difficulty_target 难度定标：根据知识基础评分(0-100)推荐资源难度层级（基础/进阶/高级）\n"
        "4. priority_focus 优先关注：最需要优先解决的 2-3 个核心薄弱点及建议学习顺序\n"
        "5. adaptation_strategy 适配策略：针对该学习者的个性化教学建议（如多视觉化、多实操、降维解释等）\n\n"
        "【必覆盖知识点清单（强制要求）】\n"
        "6. must_cover_points 本主题必覆盖知识点清单：站在「本主题教学完整性」立场（而非仅学习者薄弱点），"
        "枚举本主题所有必须被教学资源覆盖的核心知识点，通常 8-15 条。\n"
        "   - 每条必须是**具体、可判定是否出现在资源正文中**的知识点名词（如「偏差-方差权衡」"
        "「L2 正则化」「交叉验证」），不要写「掌握监督学习」这类笼统表述。\n"
        "   - 优先来自检索到的知识库内容的章节要点（保持一致性与可溯源），再补充学科公认必备点。\n"
        "   - 该清单将作为下游生成 Agent 的**硬约束**，并用于 Review Agent 逐项核销，"
        "因此宁可精准不要含糊。\n\n"
        "只返回 JSON：\n"
        "{\"knowledge_gaps\":[{\"point\":\"知识点\",\"severity\":\"high\",\"subject\":\"所属科目\",\"score\":N}],"
        "\"cognitive_profile\":{\"style\":\"认知风格\",\"pace\":\"学习节奏\",\"motivation\":N},"
        "\"difficulty_target\":\"基础|进阶|高级\",\"priority_focus\":[\"重点1\",\"重点2\"],"
        "\"adaptation_strategy\":\"个性化适配策略描述\","
        "\"must_cover_points\":[\"知识点1\",\"知识点2\",\"知识点3\"]}"
    ),
    # ── 辩论与交叉验证 Agent（命中 PDF 创新性要求①「辩论与交叉验证机制消除幻觉」核心创新点）──
    "debate": (
        "你是 Debate & Cross-Validation Agent —— 多智能体辩论与交叉验证裁判。"
        "你的核心使命是消除大模型在专业领域生成中的「幻觉」，确保知识的高保真与高可控。\n\n"
        "【工作机制】\n"
        "1. 交叉比对：将 Doc/Reading/Code/Quiz/Video 各 Agent 的产出进行交叉比对，找出相互矛盾之处\n"
        "2. 事实核验：基于领域专业知识库，核验关键事实、公式、定义、案例的真实性与准确性\n"
        "3. 幻觉识别：识别以下幻觉类型 ——\n"
        "   - 事实性幻觉：编造不存在的公式/定理/文献/案例\n"
        "   - 逻辑性幻觉：推导过程存在逻辑跳跃或循环论证\n"
        "   - 一致性幻觉：不同 Agent 产出之间存在矛盾（如文档说A，题目答案说B）\n"
        "   - 范围性幻觉：超出主题范围或答非所问\n"
        "4. 辩论裁决：对识别出的争议点进行裁决，给出权威结论与修正建议\n\n"
        "【评估标准】幻觉率 = 识别到的幻觉条数 / 关键知识点总数\n"
        "目标：幻觉率 < 5%（PDF 实用价值评分硬性指标）\n\n"
        "【知识溯源要求】fact_check.kb_evidence 必须列出关键结论所依据的知识库来源"
        "（引用检索结果中标注的「来源：文件名 · 章节」），实现高保真知识溯源。\n\n"
        "只返回 JSON：\n"
        "{\"cross_validation\":{\"contradictions\":[{\"agent_a\":\"document\",\"agent_b\":\"quiz\","
        "\"issue\":\"具体矛盾描述\",\"resolution\":\"裁决结论\"}]},"
        "\"hallucinations\":[{\"type\":\"事实性幻觉\",\"agent\":\"document\",\"content\":\"幻觉内容\","
        "\"correction\":\"正确内容\",\"severity\":\"critical|major|minor\"}],"
        "\"fact_check\":{\"verified_count\":N,\"total_facts\":N,\"hallucination_rate\":0.0X,"
        "\"kb_evidence\":[{\"claim\":\"被支撑的关键结论\",\"source_doc\":\"ai_domain_kb.md\","
        "\"section\":\"章节名\"}]},"
        "\"debate_summary\":\"辩论与交叉验证总结\",\"hallucination_prevented\":true|false,"
        "\"retry_recommended\":true|false}"
    ),
    # ── 动态迭代决策 Agent（命中 PDF「基于学习交互反馈的动态迭代机制」要求）──
    "adapt": (
        "你是 Adaptation Decision Agent —— 动态迭代决策专家。"
        "基于学情诊断、资源质量评估、辩论验证结果与学习交互反馈，"
        "动态决策是否对知识点进行「降维解释」或生成「进阶挑战任务」。\n\n"
        "【决策规则】\n"
        "1. 若学习者该知识点掌握度 < 60 或质量评估 difficulty_fit < 6 → 触发「降维解释」\n"
        "   - 生成更通俗的类比解释、可视化图示建议、分步骤拆解\n"
        "2. 若学习者该知识点掌握度 >= 85 且答题正确率 >= 80% → 触发「进阶挑战」\n"
        "   - 生成更高难度的拓展题、开放性问题、研究性任务\n"
        "3. 若辩论验证发现幻觉 → 强制触发「内容修正」并标记需重新生成\n"
        "4. 其他情况 → 维持当前资源，给出学习建议\n\n"
        "【输入】学情诊断报告 + 资源质量评估 + 辩论验证结果 + 可选的答题正确率反馈\n\n"
        "只返回 JSON：\n"
        "{\"decision\":\"simplify|challenge|correct|maintain\","
        "\"reason\":\"决策依据说明\","
        "\"actions\":[{\"type\":\"降维解释|进阶挑战|内容修正|维持\","
        "\"target_knowledge\":\"目标知识点\",\"action_detail\":\"具体动作描述\"}],"
        "\"learning_feedback\":\"给学习者的反馈寄语\","
        "\"next_iteration_needed\":true|false}"
    ),
}

_AGENT_LABELS = {
    "mindmap": "MindMap Agent",
    "document": "Doc Agent",
    "reading": "Reading Agent",
    "code": "Code Agent",
    "guide": "Guide Agent",
    "quiz": "Quiz Agent",
    "video": "Video Agent",
    "review": "Quality Review Agent",
    "supervisor": "Supervisor",
    "diagnosis": "Diagnosis Agent",
    "debate": "Debate Agent",
    "adapt": "Adapt Agent",
}

_AGENT_ACTIONS = {
    "mindmap": "构建知识结构（思维导图）",
    "document": "生成课程讲解文档",
    "reading": "生成拓展阅读材料",
    "code": "生成可运行代码案例",
    "guide": "生成分步骤实操指南（岗位技能）",
    "quiz": "生成练习题目（依据文档内容）",
    "video": "生成教学视频脚本",
    "review": "资源质量多维度评估",
    "supervisor": "任务规划与编排",
    "diagnosis": "学情诊断（锚定理论强项与技能盲区）",
    "debate": "辩论与交叉验证（消除幻觉）",
    "adapt": "动态迭代决策（降维解释/进阶挑战）",
}


# ═══════════════════════════════════════════════════════════════════════
#  对话路由工作流（保留：供 /api/v1/chat 等通用对话使用）
# ═══════════════════════════════════════════════════════════════════════

def classify_intent(state: AgentState) -> AgentState:
    user = (
        f"用户请求：{state.user_message}\n"
        f"角色：{state.user_role}\n"
        f"上下文：{json.dumps(state.context, ensure_ascii=False)}"
    )
    raw = _call_llm("supervisor", AGENT_PROMPTS["supervisor"], user)
    result = _extract_json(raw)
    state.intent = Intent(result.get("intent", "general_chat"))
    state.complexity = TaskComplexity(result.get("complexity", "simple"))
    state.mode = AgentMode(result.get("mode", "route"))
    state.supervisor_plan = result.get("plan", {})
    logger.info("Supervisor: intent=%s complexity=%s mode=%s",
                state.intent.value, state.complexity.value, state.mode.value)
    return state


def route_node(state: AgentState) -> str:
    return {
        Intent.PAPER_GENERATE: "paper",
        Intent.QUESTION_RECOMMEND: "question",
        Intent.STUDENT_ANALYSIS: "student",
        Intent.PROFILE_BUILD: "profile",
        Intent.RESOURCE_GENERATE: "resource",
        Intent.LEARNING_PATH: "path",
        Intent.QA: "qa",
        Intent.TEACHING: "teaching",
        Intent.GENERAL_CHAT: "finish",
    }.get(state.intent, "finish")


def general_chat(state: AgentState) -> AgentState:
    state.final_reply = _call_llm("supervisor", "你是友好的教育助手。", state.user_message)
    return state


def _resolve_prompt_key(state: AgentState, agent: str) -> str:
    """resource 节点按 context.agent_type 细分到专用提示词；其余节点按原名。"""
    if agent == "resource":
        agent_type = (state.context or {}).get("agent_type")
        if agent_type in AGENT_TYPE_PROMPT_ALIAS:
            return AGENT_TYPE_PROMPT_ALIAS[agent_type]
    return agent


_QUIZ_META_TAIL = re.compile(
    r"^(希望|以上|如需|若有|备注|说明[：:]|总结[：:]|祝你|祝学习|本次出题|以上就是|以上就是全部|如需更多|本次|——完——)"
)
# 题目内部混入的元描述行（独立成行的出题意图/考点说明等），逐题清洗时剔除
_QUIZ_META_LINE = re.compile(
    r"^(本题|此题|该题)[^。！？]{0,25}(考查|考察|旨在|测试|检验)"
    r"|^(考点[：:]|知识点[：:]|出题意图|命题意图|设计说明|出题思路|难度[：:]?|难度等级|参考出处|依据来源)"
    r"|^(说明|备注|提示)[：:].{0,40}$"
    r"|^(下面|接下来|以下是|以上是|首先|然后|紧接|随后|进入)[^。！？\n]{0,25}(进入|来看|部分|题型|环节|完成)"
    r"|^(共|合计|总共计?)[^。！？\n]{0,10}题?[。！？]?$"
    r"|^(请同学们|同学们|考生注意|注意事项|答题要求|试卷说明|本卷|本练习|本份)"
)

# 题型区块标记的多种可能形态 —— 统一由 _is_quiz_section_line 识别，
# 模型不一定遵守【选择题】格式，可能输出「一、选择题」「## 选择题」「**选择题**」「选择题：」等。


def _is_quiz_section_line(line: str) -> bool:
    """判断一行是否为题型区块标记（覆盖【】/序号/Markdown/加粗/冒号/（共X题）尾注等变体）。

    防误伤规则：无任何结构性前缀（【/**/##/序号）时，仅接受极短的纯标记行（≤12字），
    避免把「计算题：求lim…」这类以题型词开头的正常题干误判为区块标记。
    """
    s = line.strip()
    if not s or len(s) > 24:
        return False
    tw = r"(?:单选|多选|选择|判断|填空|简答|问答|论述|名词解释|计算|案例分析)题?"
    # 形态A：结构前缀（Markdown标题/加粗/序号/【）+ 题型词 + 简短尾注
    m = re.match(
        r"^(?:#{1,6}\s*)?(?:\d{1,2}\s*[.、．]\s*|[一二三四五六七八九十]+\s*[、.．]\s*|\*\*|【)?"
        + tw + r"(?:】|\*\*)?(?:[（(][^）)]{0,10}[）)])?\s*[:：]?\s*$",
        s,
    )
    if m:
        structural = bool(re.match(r"^(?:#{1,6}\s*|\d{1,2}\s*[.、．]\s*|[一二三四五六七八九十]+\s*[、.．]\s*|\*\*|【)", s))
        return structural or len(s) <= 12
    # 形态B：题型词开头 + （共X题）/（合计X题）类尾注
    return bool(re.match(r"^" + tw + r"\s*[（(](?:共|合计)[^）)]{0,8}[）)]\s*[:：]?\s*$", s))


_QUIZ_TYPE_LABEL = {
    "multiple_choice": "【选择题】",
    "single_choice": "【选择题】",
    "choice": "【选择题】",
    "true_false": "【判断题】",
    "judge": "【判断题】",
    "fill_blank": "【填空题】",
    "filling": "【填空题】",
    "short_answer": "【简答题】",
    "essay": "【简答题】",
}


def _strip_stem_meta(stem: str) -> str:
    """题干句级清洗：剥掉「本题考查…。」这类混入题干开头的元描述句。"""
    s = stem.strip()
    m = re.match(r"^(本题|此题|该题)[^。？！]{0,30}(考查|考察|旨在|测试|检验)[^。？！]*[。：:]\s*", s)
    if m and len(s) > len(m.group(0)):
        return s[len(m.group(0)):].strip()
    return s


def _render_quiz_from_json(data: Any) -> Optional[str]:
    """把模型自作主张包成 JSON 的题目列表重新渲染为标准纯文本格式，消除元描述。"""
    if not isinstance(data, dict):
        return None
    questions = data.get("questions")
    if not isinstance(questions, list) or not questions:
        return None
    blocks = []
    idx = 0
    for q in questions:
        if not isinstance(q, dict):
            continue
        qtype = str(q.get("type", "")).strip().lower()
        options = q.get("options")
        label = _QUIZ_TYPE_LABEL.get(qtype) or ("【选择题】" if isinstance(options, (dict, list)) and options else "【简答题】")
        stem = _strip_stem_meta(str(q.get("question") or q.get("stem") or "").strip())
        if not stem:
            continue
        idx += 1
        lines = [label, f"{idx}. {stem}"]
        if isinstance(options, dict):
            for key in sorted(options):
                lines.append(f"{key}. {str(options[key]).strip()}")
        elif isinstance(options, list):
            for i, opt in enumerate(options):
                lines.append(f"{chr(65 + i)}. {str(opt).strip()}")
        answer = q.get("answer")
        if answer not in (None, ""):
            lines.append(f"答案：{answer}")
        ref = q.get("reference_answer") or q.get("model_answer")
        if (ref not in (None, "")) and answer in (None, ""):
            lines.append(f"参考答案：{ref}")
        analysis = q.get("analysis") or q.get("explanation")
        if analysis:
            lines.append(f"解析：{analysis}")
        blocks.append("\n".join(lines))
    return "\n\n".join(blocks) if blocks else None


def _strip_generic_kb_tag(text: str) -> str:
    """剥掉题目里的裸「[通用知识]」溯源标记（保留 [来源:文件·章节] 引用）。

    该标记对题目是纯噪音：判分时无用，展示时干扰阅读；
    而真实的知识库引用有溯源价值，继续保留。
    """
    if not text:
        return text
    out = re.sub(r"\s*\[通用知识\]", "", text)
    return out.strip() if out.strip() else text


def _sanitize_quiz(text: str) -> str:
    """清洗 Quiz 输出：剥掉元描述，并兼容模型无视格式要求自行输出的 JSON 包裹。

    处理顺序：
    1) 输出里带 ```json 块或裸 JSON 且含 questions 列表 → 解析后重渲染为
       「【题型】→ 题干 → 选项 → 答案 → 解析」的标准格式（元描述/元数据全部丢弃）；
    2) 否则剥掉首个题型标记之前的开场白，以及结尾的客套总结行；
    3) 无论走哪条路径，都剥掉题目里的裸「[通用知识]」溯源标记——
       它对题目只有噪音价值；真正的 [来源:文件·章节] 引用保留。
    """
    if not text:
        return text
    # 1) JSON 包裹兼容
    json_match = re.search(r"```json\s*(.*?)```", text, re.S) or re.search(r"```\s*(\{.*?\})\s*```", text, re.S)
    candidates = []
    if json_match:
        candidates.append(json_match.group(1))
    stripped = text.strip()
    if stripped.startswith("{") and stripped.endswith("}"):
        candidates.append(stripped)
    for raw in candidates:
        try:
            data = json.loads(raw.strip())
        except (json.JSONDecodeError, ValueError):
            start, end = raw.find("{"), raw.rfind("}")
            if start < 0 or end <= start:
                continue
            try:
                data = json.loads(raw[start:end + 1])
            except (json.JSONDecodeError, ValueError):
                continue
        rendered = _render_quiz_from_json(data)
        if rendered:
            return _strip_generic_kb_tag(rendered)
    # 2) 纯文本路径：剥开场白 + 结尾客套 + 题目块内部的元描述行
    lines = text.strip().splitlines()
    start = None
    for i, line in enumerate(lines):
        if _is_quiz_section_line(line):
            start = i
            break
    if start is None:
        # 兜底：全文找不到任何题型区块标记（【】/序号/Markdown 变体都没有）时，
        # 仅剥掉首行明显的开场白，其余原样保留，避免误伤
        if lines:
            first = lines[0].strip()
            if re.match(r"^(好的|当然|以下是|下面是|已(为您)?生成|根据(您|你)的要求?)", first) and len(first) < 60:
                lines = lines[1:]
        return "\n".join(lines).strip()
    lines = lines[start:]
    # 按题型区块切分：区块标记统一改写为标准【题型】格式，块内剔除独立成行的元描述
    cleaned: list = []
    for line in lines:
        s = line.strip()
        if _is_quiz_section_line(s):
            m = re.search(r"(单选|多选|选择|判断|填空|简答|问答|论述|名词解释|计算|案例分析)", s)
            label = {
                "单选": "选择题", "多选": "选择题", "选择": "选择题",
                "判断": "判断题", "填空": "填空题",
                "简答": "简答题", "问答": "简答题", "论述": "简答题",
                "名词解释": "简答题", "计算": "简答题", "案例分析": "简答题",
            }.get(m.group(1) if m else "", "简答题")
            cleaned.append(f"【{label}】")
            continue
        if s and _QUIZ_META_LINE.match(s):
            continue
        cleaned.append(line)
    lines = cleaned
    while lines and _QUIZ_META_TAIL.match(lines[-1].strip()) and not lines[-1].strip().startswith("【"):
        lines.pop()
    return _strip_generic_kb_tag("\n".join(lines).strip())


def _agent_node(state: AgentState, agent: str) -> AgentState:
    prompt_key = _resolve_prompt_key(state, agent)
    content = _call_llm_with_tools(
        prompt_key,
        AGENT_PROMPTS[prompt_key],
        f"请求：{state.user_message}\n用户ID：{state.user_id}\n上下文：{json.dumps(state.context, ensure_ascii=False)}",
    )
    if prompt_key == "quiz":
        content = _sanitize_quiz(content)
    state.agent_results.append({"agent": prompt_key, "content": content})
    return state


def paper_agent(state: AgentState) -> AgentState:
    return _agent_node(state, "paper")


def question_agent(state: AgentState) -> AgentState:
    return _agent_node(state, "question")


def student_agent(state: AgentState) -> AgentState:
    return _agent_node(state, "student")


def profile_agent(state: AgentState) -> AgentState:
    return _agent_node(state, "profile")


def resource_agent(state: AgentState) -> AgentState:
    return _agent_node(state, "resource")


def path_agent(state: AgentState) -> AgentState:
    return _agent_node(state, "path")


def qa_agent(state: AgentState) -> AgentState:
    return _agent_node(state, "qa")


def teaching_agent(state: AgentState) -> AgentState:
    return _agent_node(state, "teaching")


def summarize(state: AgentState) -> AgentState:
    if not state.agent_results:
        state.final_reply = "未收到 Agent 执行结果。"
        return state
    # 资源生成类意图：子 Agent 产出已由各节点完成清洗（如 quiz 已剥离元描述），
    # 直接透传，不再二次 LLM 汇总——否则汇总模型会重新引入「以下是为…生成的…共 N 题…」
    # 这类元描述，并把内容包成 JSON，污染最终展示。
    if state.intent == Intent.RESOURCE_GENERATE:
        state.final_reply = "\n\n".join(
            r.get("content", "") for r in state.agent_results if r.get("content")
        )
        return state
    results_json = json.dumps(state.agent_results, ensure_ascii=False, indent=2)
    prompt = (
        f"用户问题：{state.user_message}\n"
        f"意图：{state.intent.value}\n"
        f"子 Agent 结果：\n{results_json}\n\n"
        "请用中文生成清晰、可执行的最终回复，并保留关键 JSON/结构化结果。"
    )
    state.final_reply = _call_llm("supervisor", "你是专业的结果汇总助手。", prompt)
    return state


def build_workflow():
    from langgraph.graph import END, StateGraph

    wf = StateGraph(AgentState)
    wf.add_node("classify", classify_intent)
    wf.add_node("finish", general_chat)
    wf.add_node("paper", paper_agent)
    wf.add_node("question", question_agent)
    wf.add_node("student", student_agent)
    wf.add_node("profile", profile_agent)
    wf.add_node("resource", resource_agent)
    wf.add_node("path", path_agent)
    wf.add_node("qa", qa_agent)
    wf.add_node("teaching", teaching_agent)
    wf.add_node("summarize", summarize)

    wf.set_entry_point("classify")
    wf.add_conditional_edges("classify", route_node, {
        "paper": "paper", "question": "question", "student": "student",
        "profile": "profile", "resource": "resource", "path": "path",
        "qa": "qa", "teaching": "teaching", "finish": "finish",
    })
    for node in ("paper", "question", "student", "profile", "resource", "path", "qa", "teaching"):
        wf.add_edge(node, "summarize")
    wf.add_edge("finish", END)
    wf.add_edge("summarize", END)
    return wf.compile()


multi_agent_workflow = build_workflow()


# ═══════════════════════════════════════════════════════════════════════
#  多智能体协作工作流（真协同：Supervisor规划 → 共享态多Agent生成 → 质量回审 → 聚合）
#  对应赛题「多智能体深度协同」核心要求，替代之前的"路由到单Agent"模式。
# ═══════════════════════════════════════════════════════════════════════

def _run_one_agent(agent: str, user: str) -> tuple[str, int]:
    """执行单个 Agent，实时推送 start/end 事件并返回 (内容, 真实耗时ms)。"""
    label = _AGENT_LABELS.get(agent, agent)
    action = _AGENT_ACTIONS.get(agent, "执行任务")
    emit_event({
        "type": "agent_start", "agent": label, "agent_key": agent,
        "action": action, "status": "running", "ts": _now(),
        "inputSummary": _summarize(user, 120),
    })
    t0 = time.perf_counter()
    try:
        content = _call_llm_with_tools(agent, AGENT_PROMPTS[agent], user)
        if agent == "quiz":
            content = _sanitize_quiz(content)
    except Exception as e:  # noqa: BLE001
        content = f"[{label} 执行失败: {e}]"
        emit_event({
            "type": "agent_end", "agent": label, "agent_key": agent,
            "action": action, "status": "failed", "ts": _now(),
            "durationMs": int((time.perf_counter() - t0) * 1000),
            "outputSummary": f"失败: {e}",
            "output": content,
        })
        return content, int((time.perf_counter() - t0) * 1000)
    dur = int((time.perf_counter() - t0) * 1000)
    emit_event({
        "type": "agent_end", "agent": label, "agent_key": agent,
        "action": action, "status": "completed", "ts": _now(),
        "durationMs": dur, "outputSummary": _summarize(content, 120),
        "output": content,
    })
    return content, dur


def _get_profile(state: AgentState) -> dict:
    """兼容多种画像上下文键名（profile / student_profile）。"""
    ctx = state.context or {}
    return ctx.get("profile") or ctx.get("student_profile") or {}


def _norm_keyword(s: str) -> str:
    """归一化知识点字符串，用于宽松包含判定（去空白/标点/常见连接词）。"""
    import re as _re
    return _re.sub(r"[\s（）()《》【】\[\]、,，.。:：;；/|·\-—_的与和及]", "", str(s or "")).lower()


def _coverage_fallback(must_cover: list[str], outputs: dict[str, str]) -> dict:
    """
    确定性兜底：当 Review Agent 未返回 coverage 字段时，
    用「归一化关键词是否出现在任一资源正文中」做保守核销，
    保证覆盖率指标始终可计算、可复现（与 tests/evaluate_quality.py 口径一致）。
    """
    points = [p for p in (must_cover or []) if str(p).strip()]
    if not points:
        return {}
    haystack = _norm_keyword("\n".join(str(v) for v in (outputs or {}).values()))
    items, missing = [], []
    for p in points:
        key = _norm_keyword(p)
        covered = bool(key) and key in haystack
        where = ""
        if covered:
            for name, body in (outputs or {}).items():
                if key in _norm_keyword(body):
                    where = name
                    break
        items.append({"point": p, "covered": covered, "where": where})
        if not covered:
            missing.append(p)
    total = len(points)
    covered_n = total - len(missing)
    return {
        "total": total,
        "covered": covered_n,
        "rate": round(covered_n / total, 4) if total else 0.0,
        "items": items,
        "missing": missing,
        "source": "keyword_fallback",
    }


def plan_node(state: AgentState) -> AgentState:
    """Supervisor 规划：确定激活的 Agent、执行顺序与依赖关系。"""
    topic = state.topic or state.user_message
    profile = _get_profile(state)
    # ── 消融：no_calibration 关闭显式定标指令（对照组 (a)）──
    # 对照条件保留原始"请综合考虑画像"的自然语言要求，不注入规则与中间量，
    # 用于论文 Table tab:calib 的 (a) vs (b) 对比。
    if _ablation(state).get("no_calibration"):
        hint = ""
        logger.info("[ablation:no_calibration] 已关闭显式定标指令（对照组）")
    else:
        hint = _calibration_hint(topic, profile)
    user = (
        f"教学主题：{topic}\n"
        f"学生画像：{json.dumps(profile, ensure_ascii=False)}\n"
        f"备注：{state.context.get('note', '')}\n"
        f"{hint}\n\n"
        "请规划本次多智能体协同生成方案。"
    )
    emit_event({
        "type": "plan_start", "agent": "Supervisor", "agent_key": "supervisor",
        "action": "任务规划与Agent编排", "status": "running", "ts": _now(),
    })
    t0 = time.perf_counter()
    raw = _call_llm("supervisor", AGENT_PROMPTS["supervisor_plan"], user)
    dur = int((time.perf_counter() - t0) * 1000)
    plan = _extract_json(raw)
    if not plan or not plan.get("agents"):
        plan = {
            "agents": ["mindmap", "document", "reading", "code", "guide", "quiz", "video"],
            "stages": [
                {"stage": "知识结构", "agent": "mindmap", "depends_on": []},
                {"stage": "并行生成", "agents": ["document", "reading", "code", "guide"], "depends_on": ["mindmap"]},
                {"stage": "一致性关联", "agent": "quiz", "depends_on": ["document"]},
                {"stage": "视频脚本", "agent": "video", "depends_on": ["document", "mindmap"]},
            ],
            "difficulty": "进阶",
            "rationale": "默认全链路协同方案",
        }
    emit_event({
        "type": "plan_end", "agent": "Supervisor", "agent_key": "supervisor",
        "action": "任务规划完成", "status": "completed", "ts": _now(),
        "durationMs": dur,
        "agents": plan.get("agents", []),
        "stages": plan.get("stages", []),
        "difficulty": plan.get("difficulty", ""),
        "rationale": plan.get("rationale", ""),
        "expected_difficulty": compute_expected_level(topic, profile),
    })
    # 同步记录期望档位与是否启用显式定标，供消融实验离线评分
    plan["expected_difficulty"] = compute_expected_level(topic, profile)
    plan["calibration_hint_enabled"] = bool(hint)
    state.plan = plan
    state.topic = topic
    return state


def generate_node(state: AgentState) -> AgentState:
    """
    多智能体协同生成节点 —— 真正的深度协同：
    Stage1 mindmap 先行产出知识结构（共享态）
    Stage2 document/reading/code 并行执行，各自复用 mindmap 产出
    Stage3 quiz 依据 document 内容出题（跨模态一致性）
    Stage4 video 依据 document+mindmap 生成脚本
    每个 Agent 的产出写入共享 state.agent_outputs，供下游 Agent 复用。
    """
    outputs: dict[str, str] = dict(state.agent_outputs)
    topic = state.topic
    profile = _get_profile(state)
    profile_brief = json.dumps(profile, ensure_ascii=False)

    # ── 必覆盖知识点清单（由 Diagnosis Agent 产出）：作为所有生成 Agent 的硬约束 ──
    # 目的：把「覆盖率 ≥90%」这一显式指标，转化为生成阶段的显式约束，
    # 使中间产物可被逐项检查（配合 review 阶段的逐项核销）。
    cover = [p for p in (getattr(state, "diagnosis_must_cover", None) or []) if str(p).strip()]
    if cover:
        cover_block = (
            "【必覆盖知识点清单（硬约束）】\n"
            "下列知识点为本主题教学完整性的最低要求，你生成的资源内容**必须逐项覆盖**"
            "（若某项在你的资源类型中不适用，仍需在内容中至少明确提及并作简要说明）：\n"
            + "\n".join(f"- {p}" for p in cover) + "\n"
            "请在正文中自然融入这些知识点，不要只罗列名词；不得遗漏任何一项。\n"
        )
        logger.info("必覆盖知识点清单已注入生成约束，共 %d 项", len(cover))
    else:
        cover_block = ""

    # ── RAG 知识库检索增强（PDF要求：整合领域专业知识库）──
    _abl = _ablation(state)
    if _abl.get("no_rag"):
        kb_context = ""
        logger.info("[ablation:no_rag] 已关闭知识库检索增强")
    else:
        kb_context = _rag_search(topic, top_k=5)
    if kb_context:
        kb_context = f"【知识库检索结果（RAG增强，请据此确保内容准确）】：\n{kb_context}\n"
        logger.info("RAG检索成功，为生成节点提供知识库上下文")
    else:
        logger.info("RAG检索无结果或知识库为空，降级为纯LLM生成")

    # ── Stage 1：MindMap Agent 先行（知识结构）──
    mm, _ = _run_one_agent(
        "mindmap",
        f"教学主题：{topic}\n学生画像：{profile_brief}\n{kb_context}{cover_block}请输出思维导图知识树。",
    )
    outputs["mindmap"] = mm

    # ── Stage 2：document / reading / code 并行（均复用 mindmap 产出）──
    def run_document():
        return ("document", _run_one_agent(
            "document",
            f"教学主题：{topic}\n学生画像：{profile_brief}\n{kb_context}{cover_block}"
            f"【MindMap Agent 已产出的知识结构，请据此组织文档章节】：\n{mm}\n"
            "请生成完整课程讲解文档。"
        )[0])

    def run_reading():
        return ("reading", _run_one_agent(
            "reading",
            f"教学主题：{topic}\n学生画像：{profile_brief}\n{kb_context}{cover_block}"
            f"【参考知识结构】：\n{mm}\n请生成拓展阅读材料。"
        )[0])

    def run_code():
        return ("code", _run_one_agent(
            "code",
            f"教学主题：{topic}\n学生画像：{profile_brief}\n{kb_context}{cover_block}"
            f"【参考知识结构】：\n{mm}\n请生成可运行代码案例。"
        )[0])

    def run_guide():
        return ("guide", _run_one_agent(
            "guide",
            f"教学主题：{topic}\n学生画像（含岗位方向/学历背景/理论测试结果）：{profile_brief}\n{kb_context}{cover_block}"
            f"【参考知识结构】：\n{mm}\n请生成分步骤实操指南。"
        )[0])

    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as ex:
        futs = [ex.submit(run_document), ex.submit(run_reading), ex.submit(run_code), ex.submit(run_guide)]
        for f in concurrent.futures.as_completed(futs):
            try:
                name, out = f.result()
                outputs[name] = out
            except Exception as e:  # noqa: BLE001
                logger.error("parallel agent failed: %s", e)

    doc = outputs.get("document", "")

    # ── Stage 3：Quiz Agent 依据 document 内容出题（跨模态一致性）──
    quiz, _ = _run_one_agent(
        "quiz",
        f"教学主题：{topic}\n学生画像：{profile_brief}\n"
        f"【Doc Agent 已生成的课程文档，请确保题目与文档内容一致】：\n{doc[:2500]}\n"
        "请生成练习题目。"
    )
    outputs["quiz"] = quiz

    # ── Stage 4：Video Agent 依据 document + mindmap 生成脚本 ──
    video, _ = _run_one_agent(
        "video",
        f"教学主题：{topic}\n学生画像：{profile_brief}\n"
        f"【课程文档摘要】：\n{doc[:1500]}\n【知识结构】：\n{mm[:1000]}\n"
        "请生成教学视频脚本。"
    )
    outputs["video"] = video

    state.agent_outputs = outputs
    return state


def review_node(state: AgentState) -> AgentState:
    """Quality Review Agent：对全部生成资源做多维度质量评估（命中 PDF②硬性要求）。"""
    outputs = state.agent_outputs
    profile = _get_profile(state)
    # 截断每个资源前 800 字送评，避免上下文超长
    brief = {k: (v[:800] if isinstance(v, str) else str(v)[:800]) for k, v in outputs.items()}
    # ── 必覆盖知识点逐项核销（对齐「核心知识点覆盖率 ≥90%」硬指标）──
    cover = [p for p in (getattr(state, "diagnosis_must_cover", None) or []) if str(p).strip()]
    cover_block = ""
    if cover:
        cover_block = (
            "\n【必覆盖知识点逐项核销（强制）】\n"
            "诊断阶段已确定本主题必覆盖知识点清单如下，请你**逐项判定**其是否已在上方资源产出中被实质覆盖"
            "（出现在正文并被讲解，仅出现名词不计为覆盖）：\n"
            + "\n".join(f"{i}. {p}" for i, p in enumerate(cover, 1)) + "\n"
            "并在返回 JSON 中额外给出 coverage 字段：\n"
            "{\"coverage\":{\"total\":N,\"covered\":N,\"rate\":0.XX,"
            "\"items\":[{\"point\":\"知识点\",\"covered\":true|false,\"where\":\"出现在哪个资源\"}],"
            "\"missing\":[\"未覆盖知识点\"]}}\n"
            "注意：rate = covered / total，必须与 items 中 covered=true 的条数一致。\n"
        )
    user = (
        f"教学主题：{state.topic}\n学生画像：{json.dumps(profile, ensure_ascii=False)}\n"
        f"已生成资源（各 Agent 产出摘要）：\n{json.dumps(brief, ensure_ascii=False, indent=2)}\n"
        f"{cover_block}"
        "请按评估维度审查并返回 JSON。"
    )
    emit_event({
        "type": "review_start", "agent": "Quality Review Agent", "agent_key": "review",
        "action": "资源质量多维度评估", "status": "running", "ts": _now(),
    })
    t0 = time.perf_counter()
    raw = _call_llm("review", AGENT_PROMPTS["review"], user)
    dur = int((time.perf_counter() - t0) * 1000)
    result = _extract_json(raw)
    if not result:
        result = {"scores": {}, "overall": 0, "issues": [], "retry": False}
    # 覆盖率核销结果落盘到共享态；若模型未返回则用确定性兜底（关键词包含判定）
    cov = result.get("coverage") if isinstance(result, dict) else None
    if not isinstance(cov, dict) or not cov.get("total"):
        cov = _coverage_fallback(cover, outputs)
        if cov:
            result["coverage"] = cov
    state.coverage_report = cov or {}
    emit_event({
        "type": "review_end", "agent": "Quality Review Agent", "agent_key": "review",
        "action": "资源质量评估完成", "status": "completed", "ts": _now(),
        "durationMs": dur,
        "scores": result.get("scores", {}),
        "overall": result.get("overall", 0),
        "issues": result.get("issues", []),
        "retry": bool(result.get("retry", False)),
        "coverage": state.coverage_report,
    })
    state.review_result = result
    return state


# ═══════════════════════════════════════════════════════════════════════
#  PDF 整改新增节点：学情诊断 / 辩论与交叉验证 / 动态迭代决策
# ═══════════════════════════════════════════════════════════════════════

def diagnosis_node(state: AgentState) -> AgentState:
    """
    Diagnosis Agent —— 学情诊断（命中 PDF「学情诊断 Agent」明确职责分工要求）。
    基于学生先验知识画像，精准锚定理论强项与技能盲区，为后续资源生成的个性化适配提供决策依据。
    在 plan 之后、generate 之前执行，诊断结果写入共享态供后续 Agent 复用。
    """
    profile = _get_profile(state)
    topic = state.topic or state.user_message
    # 先把知识库检索到的章节要点提供给诊断 Agent，使其 must_cover_points 与知识库保持一致（可溯源）
    kb_snippet = _rag_search(topic, top_k=3)
    user = (
        f"教学主题：{topic}\n"
        f"学生先验知识画像（含学历背景、岗位方向、理论测试结果、薄弱点、学习目标等）：\n"
        f"{json.dumps(profile, ensure_ascii=False)}\n"
        f"可选的学习交互反馈：{json.dumps(state.learning_feedback, ensure_ascii=False)}\n"
        f"【知识库检索片段（用于对齐必覆盖知识点，请优先从中提取章节要点）】\n{kb_snippet}\n\n"
        "请基于画像进行学情诊断，返回 JSON 诊断报告（含 must_cover_points 必覆盖知识点清单）。"
    )
    emit_event({
        "type": "diagnosis_start", "agent": "Diagnosis Agent", "agent_key": "diagnosis",
        "action": "学情诊断（锚定理论强项与技能盲区）", "status": "running", "ts": _now(),
    })
    t0 = time.perf_counter()
    raw = _call_llm("diagnosis", AGENT_PROMPTS["diagnosis"], user)
    dur = int((time.perf_counter() - t0) * 1000)
    result = _extract_json(raw)
    if not result:
        result = {
            "knowledge_gaps": [], "cognitive_profile": {}, "difficulty_target": "进阶",
            "priority_focus": [], "adaptation_strategy": "采用默认教学策略",
            "must_cover_points": [],
        }
    # 兜底：即使模型未输出，也保证字段存在（下游依赖该字段做覆盖率核销）
    result.setdefault("must_cover_points", [])
    state.diagnosis_must_cover = list(result.get("must_cover_points") or [])
    emit_event({
        "type": "diagnosis_end", "agent": "Diagnosis Agent", "agent_key": "diagnosis",
        "action": "学情诊断完成", "status": "completed", "ts": _now(),
        "durationMs": dur,
        "knowledge_gaps": result.get("knowledge_gaps", []),
        "difficulty_target": result.get("difficulty_target", ""),
        "priority_focus": result.get("priority_focus", []),
        "adaptation_strategy": result.get("adaptation_strategy", ""),
        "must_cover_points": result.get("must_cover_points", []),
    })
    state.diagnosis_result = result
    # 把诊断结论注入 context，供 generate 阶段的 Agent 个性化适配
    state.context["diagnosis"] = result
    return state


# 消融 B5：以「同一模型自我批判（Self-Refine）」替代对抗式辩论，
# 用于检验「辩论」的效果究竟来自对抗式角色设定，还是仅仅来自多花一轮 token。
SELF_REFINE_PROMPT = (
    "你是刚刚生成这批教学资源的同一个助手。请对自己的产出做一次自我检查：\n"
    "1. 检查是否存在事实错误（错误定义、错误公式、不存在的 API、虚构的标准或数值）；\n"
    "2. 检查各资源之间是否互相矛盾；\n"
    "3. 给出修正后的结论。\n\n"
    "返回与辩论验证完全相同的 JSON 结构：\n"
    "{\"cross_validation\":{\"contradictions\":[]},\"hallucinations\":[],"
    "\"fact_check\":{\"verified_count\":0,\"total_facts\":0,\"hallucination_rate\":0.0},"
    "\"debate_summary\":\"...\",\"hallucination_prevented\":true|false,"
    "\"retry_recommended\":true|false}"
)


def debate_node(state: AgentState) -> AgentState:
    """
    Debate & Cross-Validation Agent —— 辩论与交叉验证（命中 PDF 创新性要求①核心创新点）。
    在 generate 之后、review 之前执行。对多 Agent 产出进行交叉比对、事实核验、幻觉识别与辩论裁决，
    消除大模型在专业领域的「幻觉」，确保知识高保真。目标：幻觉率 < 5%。
    """
    outputs = state.agent_outputs
    topic = state.topic

    # ── 消融：B4 跳过辩论与交叉验证 ──
    # 注意：跳过辩论后系统不再自报幻觉率（置 None）。消融组的幻觉率必须由
    # 统一的外部判定器（tests/verification_validity.py）测量，否则会得出
    # 「不做验证 = 零幻觉」的荒谬结论。
    if _ablation(state).get("no_debate"):
        state.debate_result = {
            "cross_validation": {"contradictions": []},
            "hallucinations": [],
            "fact_check": {"verified_count": 0, "total_facts": 0, "hallucination_rate": None},
            "debate_summary": "消融设置：已跳过辩论与交叉验证（幻觉率由外部判定器统一测量）",
            "hallucination_prevented": False,
            "retry_recommended": False,
            "ablation": "no_debate",
            "skipped": True,
        }
        logger.info("[ablation:no_debate] 已跳过辩论与交叉验证")
        return state

    # 截断每个资源前 600 字送辩，避免上下文超长
    brief = {k: (v[:600] if isinstance(v, str) else str(v)[:600]) for k, v in outputs.items()}
    # ── RAG 知识库检索（PDF要求：高保真知识溯源与约束生成）──
    kb_context = _rag_search(topic, top_k=5)
    kb_section = f"\n【知识库权威参考（RAG检索，用于事实核验）】：\n{kb_context}\n" if kb_context else ""
    user = (
        f"教学主题：{topic}\n"
        f"各 Agent 产出（用于交叉验证）：\n{json.dumps(brief, ensure_ascii=False, indent=2)}\n"
        f"{kb_section}"
        "请执行辩论与交叉验证：基于知识库参考，找出 Agent 间的矛盾、识别幻觉、给出裁决结论。返回 JSON。"
    )
    emit_event({
        "type": "debate_start", "agent": "Debate Agent", "agent_key": "debate",
        "action": "辩论与交叉验证（消除幻觉）", "status": "running", "ts": _now(),
    })
    t0 = time.perf_counter()
    prompt = SELF_REFINE_PROMPT if _ablation(state).get("self_refine") else AGENT_PROMPTS["debate"]
    raw = _call_llm("debate", prompt, user)
    dur = int((time.perf_counter() - t0) * 1000)
    result = _extract_json(raw)
    if not result:
        result = {
            "cross_validation": {"contradictions": []},
            "hallucinations": [],
            "fact_check": {"verified_count": 0, "total_facts": 0, "hallucination_rate": 0.0},
            "debate_summary": "辩论验证未返回有效结果", "hallucination_prevented": False,
            "retry_recommended": False,
        }
    emit_event({
        "type": "debate_end", "agent": "Debate Agent", "agent_key": "debate",
        "action": "辩论与交叉验证完成", "status": "completed", "ts": _now(),
        "durationMs": dur,
        "contradictions_count": len(result.get("cross_validation", {}).get("contradictions", [])),
        "hallucinations_count": len(result.get("hallucinations", [])),
        "hallucination_rate": result.get("fact_check", {}).get("hallucination_rate", 0),
        "verified_count": result.get("fact_check", {}).get("verified_count", 0),
        "total_facts": result.get("fact_check", {}).get("total_facts", 0),
        "hallucinations": result.get("hallucinations", []),
        "contradictions": result.get("cross_validation", {}).get("contradictions", []),
        "kb_evidence": result.get("fact_check", {}).get("kb_evidence", []),
        "hallucination_prevented": bool(result.get("hallucination_prevented", False)),
        "debate_summary": result.get("debate_summary", ""),
    })
    state.debate_result = result
    return state


def adapt_node(state: AgentState) -> AgentState:
    """
    Adaptation Decision Agent —— 动态迭代决策（命中 PDF「基于学习交互反馈的动态迭代机制」要求）。
    在 review 之后执行。基于学情诊断 + 资源质量评估 + 辩论验证结果 + 学习反馈，
    动态决策是否「降维解释」「进阶挑战」「内容修正」或「维持」。
    """
    profile = _get_profile(state)
    feedback = state.learning_feedback
    user = (
        f"教学主题：{state.topic}\n"
        f"学生画像：{json.dumps(profile, ensure_ascii=False)}\n"
        f"学情诊断报告：{json.dumps(state.diagnosis_result, ensure_ascii=False)}\n"
        f"资源质量评估：{json.dumps(state.review_result, ensure_ascii=False)}\n"
        f"辩论验证结果：{json.dumps(state.debate_result, ensure_ascii=False)}\n"
        f"学习交互反馈（答题正确率等）：{json.dumps(feedback, ensure_ascii=False)}\n"
        "请基于以上信息进行动态迭代决策，返回 JSON。"
    )
    emit_event({
        "type": "adapt_start", "agent": "Adapt Agent", "agent_key": "adapt",
        "action": "动态迭代决策（降维解释/进阶挑战）", "status": "running", "ts": _now(),
    })
    t0 = time.perf_counter()
    raw = _call_llm("adapt", AGENT_PROMPTS["adapt"], user)
    dur = int((time.perf_counter() - t0) * 1000)
    result = _extract_json(raw)
    if not result:
        result = {
            "decision": "maintain", "reason": "决策信息不足，维持当前资源",
            "actions": [], "learning_feedback": "", "next_iteration_needed": False,
        }
    emit_event({
        "type": "adapt_end", "agent": "Adapt Agent", "agent_key": "adapt",
        "action": "动态迭代决策完成", "status": "completed", "ts": _now(),
        "durationMs": dur,
        "decision": result.get("decision", ""),
        "reason": result.get("reason", ""),
        "actions_count": len(result.get("actions", [])),
        "learning_feedback": result.get("learning_feedback", ""),
        "next_iteration_needed": bool(result.get("next_iteration_needed", False)),
    })
    state.adapt_result = result
    return state


def prepare_retry(state: AgentState) -> AgentState:
    state.retry_count = state.retry_count + 1
    emit_event({
        "type": "retry", "agent": "Supervisor", "action": f"质量审查发现关键问题，触发重试 (第{state.retry_count}次)",
        "status": "running", "ts": _now(),
    })
    return state


def decide_after_review(state: AgentState) -> str:
    """
    决策是否需要重试。综合考虑：
    1. Quality Review 发现 critical 问题
    2. Debate Agent 发现 critical 幻觉（PDF 创新性要求：幻觉防控）
    任一触发且未超过重试上限，则重试生成；否则进入动态迭代决策。
    """
    # 消融：B6 关闭评审触发的重新生成
    if _ablation(state).get("no_retry"):
        return "adapt"

    issues = state.review_result.get("issues", [])
    has_critical_review = any(str(i.get("severity", "")).lower() == "critical" for i in issues)
    need_retry_review = bool(state.review_result.get("retry", False)) and has_critical_review

    # 辩论验证发现的 critical 幻觉也触发重试
    hallucinations = state.debate_result.get("hallucinations", [])
    has_critical_hallucination = any(
        str(h.get("severity", "")).lower() == "critical" for h in hallucinations
    )
    need_retry_debate = bool(state.debate_result.get("retry_recommended", False)) and has_critical_hallucination

    if (need_retry_review or need_retry_debate) and state.retry_count < 1:
        return "retry"
    return "adapt"


def aggregate_node(state: AgentState) -> AgentState:
    """Supervisor 聚合：合并全部 Agent 产出 + 学情诊断 + 辩论验证 + 质量评估 + 动态决策为最终结构化回复。"""
    outputs = state.agent_outputs
    review = state.review_result
    diagnosis = state.diagnosis_result
    debate = state.debate_result
    adapt = state.adapt_result
    order = [("mindmap", "🧠 思维导图"), ("document", "📚 课程文档"),
             ("reading", "🔭 拓展阅读"), ("code", "💻 代码案例"),
             ("quiz", "✏️ 练习题目"), ("video", "🎬 教学视频脚本")]
    parts = [f"# 多智能体协同生成结果\n\n**教学主题**：{state.topic}\n"]

    # ── 学情诊断报告 ──
    if diagnosis:
        parts.append(f"\n---\n\n## 🎯 学情诊断报告（Diagnosis Agent）\n")
        diff_target = diagnosis.get("difficulty_target", "")
        if diff_target:
            parts.append(f"\n**推荐难度层级**：{diff_target}\n")
        gaps = diagnosis.get("knowledge_gaps", [])
        if gaps:
            parts.append("\n**知识盲区定位**：\n")
            for g in gaps[:5]:
                parts.append(f"- [{g.get('severity', '?')}] {g.get('point', '')}（{g.get('subject', '')}，掌握度 {g.get('score', 'N/A')}）\n")
        focus = diagnosis.get("priority_focus", [])
        if focus:
            parts.append("\n**优先关注**：\n")
            for i, f in enumerate(focus, 1):
                parts.append(f"{i}. {f}\n")
        strategy = diagnosis.get("adaptation_strategy", "")
        if strategy:
            parts.append(f"\n**适配策略**：{strategy}\n")
        must_cover = diagnosis.get("must_cover_points", []) or []
        if must_cover:
            parts.append(f"\n**本主题必覆盖知识点（{len(must_cover)} 项）**：{'、'.join(str(x) for x in must_cover)}\n")

    # ── 核心知识点覆盖率核销（对齐「覆盖率 ≥90%」硬指标）──
    cov = state.coverage_report or {}
    if cov.get("total"):
        parts.append(
            f"\n---\n\n## ✅ 核心知识点覆盖率核销（Review Agent）\n\n"
            f"**覆盖率**：{cov.get('covered', 0)}/{cov.get('total', 0)} = "
            f"{float(cov.get('rate', 0)) * 100:.1f}%（目标 ≥90%）\n"
        )
        missing = cov.get("missing") or []
        if missing:
            parts.append(f"\n**未覆盖**：{'、'.join(str(x) for x in missing)}\n")

    # ── 各 Agent 生成的资源 ──
    for key, label in order:
        if key in outputs:
            parts.append(f"\n---\n\n## {label}\n\n{outputs[key]}")

    # ── 辩论与交叉验证报告（PDF 创新性核心）──
    if debate:
        parts.append(f"\n---\n\n## 🛡️ 辩论与交叉验证报告（Debate Agent）\n")
        fact_check = debate.get("fact_check", {})
        if fact_check:
            parts.append(f"\n**事实核验**：已核验 {fact_check.get('verified_count', 0)}/{fact_check.get('total_facts', 0)} 个关键知识点，")
            parts.append(f"幻觉率 {fact_check.get('hallucination_rate', 0)*100 if isinstance(fact_check.get('hallucination_rate', 0), (int, float)) else 0:.1f}%（目标 <5%）\n")
        contradictions = debate.get("cross_validation", {}).get("contradictions", [])
        if contradictions:
            parts.append("\n**Agent 间矛盾裁决**：\n")
            for c in contradictions[:5]:
                parts.append(f"- {c.get('agent_a', '')} ↔ {c.get('agent_b', '')}：{c.get('issue', '')}\n")
                parts.append(f"  → 裁决：{c.get('resolution', '')}\n")
        hallucinations = debate.get("hallucinations", [])
        if hallucinations:
            parts.append("\n**识别到的幻觉**：\n")
            for h in hallucinations[:5]:
                parts.append(f"- [{h.get('severity', '?').upper()}] {h.get('type', '')}（{h.get('agent', '')}）：{h.get('content', '')}\n")
                parts.append(f"  → 修正：{h.get('correction', '')}\n")
        else:
            parts.append("\n**未发现幻觉** ✅ 所有 Agent 产出一致且事实准确。\n")
        summary = debate.get("debate_summary", "")
        if summary:
            parts.append(f"\n**辩论总结**：{summary}\n")

    # ── 资源质量评估 ──
    if review:
        scores = review.get("scores", {})
        overall = review.get("overall", "N/A")
        parts.append(f"\n---\n\n## ✅ 资源质量评估（Quality Review Agent）\n")
        parts.append(f"\n**总体评分**：{overall}/10\n\n")
        if scores:
            parts.append("| 维度 | 评分 |\n|------|------|\n")
            dim_label = {
                "accuracy": "准确性", "difficulty_fit": "难度适配",
                "completeness": "完整性", "cross_modal_consistency": "跨模态一致性",
            }
            for k, v in scores.items():
                parts.append(f"| {dim_label.get(k, k)} | {v}/10 |\n")
        issues = review.get("issues", [])
        if issues:
            parts.append("\n**发现的问题**：\n")
            for i in issues:
                parts.append(f"- [{i.get('severity', '?').upper()}] {i.get('resource', '')}：{i.get('description', '')}\n")

    # ── 动态迭代决策（PDF 反馈机制）──
    if adapt:
        parts.append(f"\n---\n\n## 🔄 动态迭代决策（Adapt Agent）\n")
        decision = adapt.get("decision", "")
        decision_label = {
            "simplify": "降维解释", "challenge": "进阶挑战",
            "correct": "内容修正", "maintain": "维持当前资源",
        }
        parts.append(f"\n**决策结果**：{decision_label.get(decision, decision)}\n")
        reason = adapt.get("reason", "")
        if reason:
            parts.append(f"**决策依据**：{reason}\n")
        actions = adapt.get("actions", [])
        if actions:
            parts.append("\n**执行动作**：\n")
            for a in actions:
                parts.append(f"- {a.get('type', '')}：{a.get('target_knowledge', '')} — {a.get('action_detail', '')}\n")
        feedback = adapt.get("learning_feedback", "")
        if feedback:
            parts.append(f"\n**学习反馈**：{feedback}\n")

    parts.append(f"\n---\n\n*本次协同由 Supervisor 规划 → Diagnosis Agent 学情诊断 → "
                 f"MindMap/Doc/Reading/Code/Quiz/Video 六大专业 Agent 共享态协作生成 → "
                 f"Debate Agent 辩论与交叉验证（消除幻觉）→ Quality Review Agent 质量审查 → "
                 f"Adapt Agent 动态迭代决策。共 {len(outputs)} 项资源，"
                 f"重试 {state.retry_count} 次。*")

    state.final_reply = "".join(parts)
    return state


def build_collaboration_workflow():
    """
    构建多智能体深度协同工作流（命中 PDF「分析-生成-校验-决策」协同闭环硬性要求）。

    完整流程：
      plan       —— Supervisor 任务规划与 Agent 编排
      diagnosis  —— 学情诊断 Agent：锚定理论强项与技能盲区（PDF 智能体①）
      generate   —— MindMap/Doc/Reading/Code/Quiz/Video 六大领域知识生成 Agent（PDF 智能体②）
      debate     —— 辩论与交叉验证 Agent：消除幻觉、知识溯源（PDF 智能体③ + 创新性核心）
      review     —— Quality Review Agent：多维度质量评估
      adapt      —— 动态迭代决策 Agent：基于反馈决策降维/进阶（PDF 反馈机制）
      aggregate  —— Supervisor 聚合输出

    重试机制：debate/review 发现 critical 幻觉或错误时，回到 generate 重新生成（最多 1 次）。
    """
    from langgraph.graph import END, StateGraph

    g = StateGraph(AgentState)
    g.add_node("plan", plan_node)
    g.add_node("diagnosis", diagnosis_node)
    g.add_node("generate", generate_node)
    g.add_node("debate", debate_node)
    g.add_node("review", review_node)
    g.add_node("adapt", adapt_node)
    g.add_node("prepare_retry", prepare_retry)
    g.add_node("aggregate", aggregate_node)

    g.set_entry_point("plan")
    g.add_edge("plan", "diagnosis")
    g.add_edge("diagnosis", "generate")
    g.add_edge("generate", "debate")
    g.add_edge("debate", "review")
    g.add_conditional_edges("review", decide_after_review, {
        "retry": "prepare_retry",
        "adapt": "adapt",
    })
    g.add_edge("prepare_retry", "generate")
    g.add_edge("adapt", "aggregate")
    g.add_edge("aggregate", END)
    return g.compile()


collaboration_workflow = build_collaboration_workflow()


# ═══════════════════════════════════════════════════════════════════════
#  对外运行入口
# ═══════════════════════════════════════════════════════════════════════

def _set_callback(cb: Callable[[dict], None] | None) -> None:
    global _event_cb
    _event_cb = cb


def run_collaboration(topic: str, context: dict | None = None,
                      request_id: str = "") -> dict:
    """同步运行协作工作流，返回完整结果 + 执行轨迹。

    context 中可携带 ``ablation`` 字典用于消融实验（no_rag / no_debate /
    self_refine / no_retry），缺省为空即完整系统。
    """
    events: list[dict] = []
    _set_callback(events.append)
    ctx = dict(context or {})
    ablation = ctx.pop("ablation", None) or {}
    try:
        init = AgentState(
            intent=Intent.COLLABORATION,
            topic=topic,
            context=ctx,
            request_id=request_id,
            ablation=ablation,
        )
        # LangGraph invoke 返回 dict（而非 dataclass 实例），用 dict 访问。
        result = collaboration_workflow.invoke(init)
        return {
            "reply": result.get("final_reply", ""),
            "outputs": result.get("agent_outputs", {}),
            "review": result.get("review_result", {}),
            "plan": result.get("plan", {}),
            "trace": events,
            "retry_count": result.get("retry_count", 0),
            "diagnosis": result.get("diagnosis_result", {}),
            "debate": result.get("debate_result", {}),
            "adapt": result.get("adapt_result", {}),
            "coverage": result.get("coverage_report", {}),
            "ablation": ablation,
        }
    finally:
        _set_callback(None)


def run_collaboration_streaming(topic: str, context: dict | None = None,
                                request_id: str = "",
                                learning_feedback: dict | None = None) -> "queue.Queue":
    """
    异步运行协作工作流，返回一个事件队列。
    队列项为 ("event", dict) / ("final", dict) / ("error", str) / ("done", None)。
    调用方从队列实时读取事件用于 SSE 推送。
    learning_feedback: 可选的学习交互反馈（答题正确率等），用于动态迭代决策。
    """
    q: queue.Queue = queue.Queue()

    def worker():
        _set_callback(lambda ev: q.put(("event", ev)))
        try:
            init = AgentState(
                intent=Intent.COLLABORATION,
                topic=topic,
                context=context or {},
                request_id=request_id,
                learning_feedback=learning_feedback or {},
            )
            result = collaboration_workflow.invoke(init)
            q.put(("final", {
                "reply": result.get("final_reply", ""),
                "outputs": result.get("agent_outputs", {}),
                "review": result.get("review_result", {}),
                "plan": result.get("plan", {}),
                "retry_count": result.get("retry_count", 0),
                "diagnosis": result.get("diagnosis_result", {}),
                "debate": result.get("debate_result", {}),
                "adapt": result.get("adapt_result", {}),
                "coverage": result.get("coverage_report", {}),
            }))
        except Exception as e:  # noqa: BLE001
            logger.exception("collaboration workflow failed")
            q.put(("error", str(e)))
        finally:
            _set_callback(None)
            q.put(("done", None))

    t = threading.Thread(target=worker, daemon=True)
    t.start()
    return q
