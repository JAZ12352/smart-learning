"""
垂直领域知识库切片导入脚本（多文件版）
将 knowledge_base/ 目录下全部 Markdown 切片索引到 ChromaDB，
供多智能体协同决策系统的 RAG 检索使用。

改进点：
1. 自动扫描 knowledge_base/*.md 全部切片，逐文件入库并携带来源元数据（支持知识溯源）
2. 按 Markdown 标题语义分块（## 级），长章节再滑动窗口细分，避免跨主题污染检索
3. 每个块记录 source / section 元数据，供 Debate Agent 与前端引用溯源展示

用法: python import_kb.py
"""
import os
import re
import sys

# 添加当前目录到 path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from infrastructure.providers import get_chroma_collection
from infrastructure.embedding import OfflineEmbeddingFunction
import chromadb
from chromadb.config import Settings


KB_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "knowledge_base")
COLLECTION_NAME = "xzs_knowledge"
PERSIST_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "chroma_data")


def split_by_sections(text: str) -> list[tuple[str, str]]:
    """按 ## 级标题把文档切成 (section_title, section_text) 列表。"""
    lines = text.splitlines()
    sections: list[tuple[str, str]] = []
    title, buf = "_header", []
    for line in lines:
        if re.match(r"^#{2}\s+", line.strip()):
            if buf:
                sections.append((title, "\n".join(buf).strip()))
            title, buf = line.strip().lstrip("#").strip(), [line]
        else:
            buf.append(line)
    if buf:
        sections.append((title, "\n".join(buf).strip()))
    return [(t, s) for t, s in sections if s]


def chunk_text(text: str, chunk_size: int = 500, overlap: int = 50) -> list[str]:
    """将文本按字符数分块，块间有重叠。"""
    text = text.strip()
    if not text:
        return []
    chunks = []
    start = 0
    while start < len(text):
        end = start + chunk_size
        chunks.append(text[start:end])
        start = end - overlap
        if start >= len(text):
            break
        if start <= 0:
            start = end
    return chunks


def iter_kb_files() -> list[str]:
    files = sorted(
        f for f in os.listdir(KB_DIR)
        if f.endswith(".md") and not f.startswith(".")
    )
    return files


def main():
    kb_files = iter_kb_files()
    if not kb_files:
        print(f"[ERROR] 知识库目录为空: {KB_DIR}")
        sys.exit(1)

    # 初始化 ChromaDB 客户端（离线确定性嵌入，避免下载 ONNX 模型）
    client = chromadb.Client(Settings(persist_directory=PERSIST_DIR, is_persistent=True))
    ef = OfflineEmbeddingFunction()

    # 创建或获取集合
    try:
        col = client.get_collection(COLLECTION_NAME, embedding_function=ef)
        print(f"[INFO] 使用已有集合: {COLLECTION_NAME} (当前 {col.count()} 条记录)")
    except Exception:
        col = client.create_collection(COLLECTION_NAME, embedding_function=ef)
        print(f"[INFO] 创建新集合: {COLLECTION_NAME}")

    total_chunks = 0
    for fname in kb_files:
        path = os.path.join(KB_DIR, fname)
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        print(f"\n[INFO] 加载知识库切片: {fname} ({len(content)} 字符)")

        # 先删除该来源旧数据
        try:
            existing = col.get(where={"source": fname})
            if existing and existing.get("ids"):
                col.delete(ids=existing["ids"])
                print(f"[INFO] 删除旧数据: {len(existing['ids'])} 条")
        except Exception as e:
            print(f"[WARN] 删除旧数据失败（可忽略）: {e}")

        # 标题语义分块 + 长段滑窗细分
        ids, docs, metas = [], [], []
        idx = 0
        for sec_title, sec_body in split_by_sections(content):
            pieces = [sec_body] if len(sec_body) <= 500 else chunk_text(sec_body, 500, 50)
            for piece in pieces:
                ids.append(f"{os.path.splitext(fname)[0]}_chunk_{idx}")
                docs.append(piece)
                metas.append({
                    "source": fname,
                    "domain": "人工智能",
                    "section": sec_title,
                    "chunk_index": idx,
                    "doc_type": "vertical_domain_kb",
                })
                idx += 1

        col.add(documents=docs, metadatas=metas, ids=ids)
        total_chunks += len(docs)
        print(f"[SUCCESS] {fname} 入库 {len(docs)} 个文本块")

    print(f"\n[DONE] 全部切片导入完成，共 {total_chunks} 个文本块")
    print(f"[INFO] 集合总记录数: {col.count()}")

    # 验证检索
    for q in ["Transformer 自注意力机制", "LoRA 微调显存要求", "预测性维护 报警精确率"]:
        results = col.query(query_texts=[q], n_results=2)
        print(f"\n[INFO] 验证检索（query='{q}'）:")
        for i, (doc, meta) in enumerate(zip(results["documents"][0], results["metadatas"][0])):
            print(f"  [{i+1}] {meta['source']} | {meta['section']}: {doc[:60]}...")


if __name__ == "__main__":
    main()
