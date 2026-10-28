"""
离线确定性嵌入函数（Offline Deterministic Embedding）

用途：在无法访问外网（无法下载 all-MiniLM-L6-v2 ONNX 模型）时，
为 ChromaDB 提供一种确定性、无外部依赖的文本向量化方案，
保证「垂直领域知识库」的入库与检索在全离线环境下可复现运行。

算法：字符 n-gram（unigram + bigram）散列到固定维度向量，再做 L2 归一化。
    - 对中文按「字」切分，避免分词依赖
    - 同一文本永远产生同一向量（可复现）
    - 相似文本（共享较多字符/词组）向量夹角更小，可支撑语义近似检索
"""
from __future__ import annotations

import hashlib
import math

DIM = 384


def _embed(text: str) -> list[float]:
    vec = [0.0] * DIM
    if not text:
        return vec
    text = str(text).lower().strip()
    chars = list(text)

    def bump(gram: str) -> None:
        h = int(hashlib.md5(gram.encode("utf-8")).hexdigest(), 16)
        vec[h % DIM] += 1.0

    # unigram + bigram
    for i, ch in enumerate(chars):
        bump(ch)
        if i + 1 < len(chars):
            bump(ch + chars[i + 1])

    norm = math.sqrt(sum(v * v for v in vec))
    if norm == 0:
        return vec
    return [v / norm for v in vec]


class OfflineEmbeddingFunction:
    """可传入 ChromaDB 的离线嵌入函数（实现 EmbeddingFunction 协议）。"""

    def __init__(self, dim: int = DIM) -> None:
        self._dim = dim

    def name(self) -> str:
        return "offline-ngram-hash"

    def __call__(self, input):
        return [_embed(t) for t in input]

    def embed_query(self, input):
        return [_embed(t) for t in input]

    def embed_documents(self, input):
        return [_embed(t) for t in input]

    def get_config(self) -> dict:
        return {"dim": self._dim}

    @staticmethod
    def build_from_config(config: dict) -> "OfflineEmbeddingFunction":
        return OfflineEmbeddingFunction(dim=int(config.get("dim", DIM)))


# 便于外部 import 的单例
offline_ef = OfflineEmbeddingFunction()
