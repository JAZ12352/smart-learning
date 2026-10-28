"""┌─────────────────────────────────────────────────────┐
│  基础设施层 — MySQL + Java API + ChromaDB           │
└─────────────────────────────────────────────────────┘"""
from __future__ import annotations

from models.config import config


def get_mysql_engine():
    """MySQL 只读连接引擎 (SQLAlchemy)"""
    from sqlalchemy import create_engine
    cfg = config.mysql_readonly
    url = f"mysql+pymysql://{cfg['user']}:{cfg['password']}@{cfg['host']}:{cfg['port']}/{cfg['database']}"
    return create_engine(url, pool_pre_ping=True, pool_size=5, max_overflow=10)


def query_db(sql: str, params: dict | None = None) -> list[dict]:
    """执行只读 SQL 查询"""
    from sqlalchemy import text
    engine = get_mysql_engine()
    try:
        with engine.connect() as conn:
            rows = conn.execute(text(sql), params or {})
            return [dict(row._mapping) for row in rows]
    finally:
        engine.dispose()


def call_java(endpoint: str, method: str = "POST", payload: dict | None = None) -> dict:
    """调用 Java 后端 API（写操作走 Java）"""
    import httpx
    cfg = config.java_backend
    url = f"{cfg['base_url']}/{endpoint.lstrip('/')}"
    with httpx.Client(timeout=30) as client:
        resp = client.request(method.upper(), url, json=payload) if method.upper() != "GET" \
            else client.get(url, params=payload)
        resp.raise_for_status()
        return resp.json()


def get_chroma_collection(name: str | None = None):
    """获取 ChromaDB 集合（离线确定性嵌入，避免外网下载 ONNX 模型）"""
    import chromadb
    from chromadb.config import Settings
    from infrastructure.embedding import OfflineEmbeddingFunction
    cfg = config.chromadb
    client = chromadb.Client(Settings(
        persist_directory=cfg["persist_dir"], is_persistent=True))
    col_name = name or cfg["collection_name"]
    try:
        return client.get_collection(col_name, embedding_function=OfflineEmbeddingFunction())
    except Exception:
        try:
            return client.get_or_create_collection(col_name, embedding_function=OfflineEmbeddingFunction())
        except Exception:
            return None


def search_vectors(query: str, top_k: int = 5, collection: str | None = None) -> list[dict]:
    """混合检索：BM25 字符级关键词（主） + ChromaDB 向量（辅），带相关度阈值过滤。

    原纯向量检索使用 n-gram 哈希嵌入（无语义能力），常返回与查询主题无关的片段，
    导致 LLM-as-Judge 拿错误参考资料误判。混合方案：
      1. BM25（中文按字 bigram 切分）精准匹配领域专名（如「反向传播」「监督学习」）
      2. 归一化相关度低于 MIN_RELEVANCE 的结果剔除——宁返回空，不给 judge 垃圾片段
    """
    col = get_chroma_collection(collection)
    if col is None:
        return []
    if not query or not query.strip():
        return []

    MIN_RELEVANCE = 0.2
    scores: dict[int, float] = {}

    # ── 1. BM25 字符级检索 ──
    corpus = _get_bm25_index(col)
    if corpus:
        bm25_scores = _bm25_score(query, corpus)
        max_s = max(bm25_scores.values()) if bm25_scores else 0.0
        if max_s > 0:
            for idx, s in bm25_scores.items():
                scores[idx] = scores.get(idx, 0.0) + 0.8 * (s / max_s)

    # ── 2. 向量检索（哈希嵌入，仅作补充信号） ──
    try:
        vres = col.query(query_texts=[query], n_results=min(top_k * 3, 20))
        docs = vres.get("documents") or [[]]
        dists = vres.get("distances") or [[]]
        metas = vres.get("metadatas") or [[]]
        ids = vres.get("ids") or [[]]
        if docs[0]:
            min_d = min(dists[0]) if dists[0] else 0.0
            for i, d in enumerate(dists[0]):
                # 哈希嵌入的 distance 区分度有限，给低权重
                rel = max(0.0, 1.0 - d) if min_d >= 0 else 0.0
                idx = ids[0][i] if ids and ids[0] and i < len(ids[0]) else None
                if idx is not None:
                    scores[idx] = scores.get(idx, 0.0) + 0.2 * rel
    except Exception:
        pass

    if not scores:
        return []

    # ── 3. 取 top_k 并做相关度阈值过滤 ──
    ranked = sorted(scores.items(), key=lambda kv: kv[1], reverse=True)
    # BM25 归一化分（0~1）与向量补充分（0~0.2）合并后，阈值取绝对值判断
    filtered = [(i, s) for i, s in ranked if s >= MIN_RELEVANCE][:top_k]
    if not filtered:
        return []

    # 用 id → doc/meta 映射取回内容
    id2doc, id2meta = corpus["id2doc"], corpus["id2meta"]
    out = []
    for idx, s in filtered:
        doc = id2doc.get(idx)
        if doc is None:
            continue
        out.append({
            "content": doc,
            "metadata": id2meta.get(idx, {}),
            "score": round(1.0 - min(s, 1.0), 4),  # 兼容原「distance」语义：越小越相关
            "relevance": round(min(s, 1.0), 4),
        })
    return out


# ── BM25 字符级索引（模块级缓存，按集合 count 变化自动重建） ──

_BM25_CACHE: dict[str, dict] = {}


def _tokenize(text: str) -> list[str]:
    """中文按字 bigram、英文/数字按完整词切分。"""
    import re
    tokens: list[str] = []
    for seg in re.findall(r"[\u4e00-\u9fff]+|[a-zA-Z]+|\d+", str(text).lower()):
        if re.match(r"[\u4e00-\u9fff]", seg):
            if len(seg) == 1:
                tokens.append(seg)
            else:
                tokens.extend(seg[i:i + 2] for i in range(len(seg) - 1))
        else:
            tokens.append(seg)
    return tokens


def _get_bm25_index(col) -> dict | None:
    """构建/获取集合的 BM25 索引缓存（count 变化时自动失效重建）。"""
    name = col.name
    count = col.count()
    cached = _BM25_CACHE.get(name)
    if cached and cached.get("count") == count:
        return cached
    try:
        data = col.get(include=["documents", "metadatas"])
    except Exception:
        return None
    ids = data.get("ids") or []
    docs = data.get("documents") or []
    metas = data.get("metadatas") or []
    if not ids:
        return None

    k1, b = 1.5, 0.75
    tf_list: list[dict[str, int]] = []
    df: dict[str, int] = {}
    doc_len: list[int] = []
    for doc in docs:
        toks = _tokenize(doc or "")
        doc_len.append(len(toks) or 1)
        tf: dict[str, int] = {}
        for t in toks:
            tf[t] = tf.get(t, 0) + 1
        tf_list.append(tf)
        for t in tf:
            df[t] = df.get(t, 0) + 1

    avgdl = sum(doc_len) / len(doc_len) if doc_len else 1.0
    n = len(ids)
    idx = {
        "count": count,
        "ids": ids,
        "tf_list": tf_list,
        "df": df,
        "doc_len": doc_len,
        "avgdl": avgdl,
        "n": n,
        "k1": k1,
        "b": b,
        "id2doc": dict(zip(ids, docs)),
        "id2meta": dict(zip(ids, metas)),
    }
    _BM25_CACHE[name] = idx
    return idx


def _bm25_score(query: str, corpus: dict) -> dict[str, float]:
    """BM25 打分，返回 {chunk_id: score}。"""
    q_tokens = _tokenize(query)
    if not q_tokens:
        return {}
    k1, b = corpus["k1"], corpus["b"]
    n, avgdl = corpus["n"], corpus["avgdl"]
    df, tf_list, doc_len = corpus["df"], corpus["tf_list"], corpus["doc_len"]
    scores: dict[str, float] = {}
    for i, tf in enumerate(tf_list):
        score = 0.0
        for t in set(q_tokens):
            f = tf.get(t)
            if not f:
                continue
            idf = max(0.01, (n - df.get(t, 0) + 0.5) / (df.get(t, 0) + 0.5))
            score += idf * (f * (k1 + 1)) / (f + k1 * (1 - b + b * doc_len[i] / avgdl))
        if score > 0:
            scores[corpus["ids"][i]] = score
    return scores
