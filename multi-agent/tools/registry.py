"""┌─────────────────────────────────────────────────────┐
│  工具注册中心 — Agent 通过它获取可用工具             │
│  每个 Agent 在启动时注册其需要的工具                  │
└─────────────────────────────────────────────────────┘"""
from __future__ import annotations

import json
from typing import Callable

from infrastructure.providers import query_db, search_vectors


def _make_db_tool():
    """创建数据库查询工具"""
    from langchain.tools import tool

    @tool
    def sql_query(sql: str) -> str:
        """执行 MySQL 只读查询。用于查询题库、学生数据、考试记录等。参数: SQL 语句。"""
        try:
            rows = query_db(sql)
            return json.dumps(rows, ensure_ascii=False, default=str)
        except Exception as e:
            return f"查询失败: {e}"
    return sql_query

def _make_search_tool():
    from langchain.tools import tool

    @tool
    def knowledge_search(query: str) -> str:
        """在向量知识库中检索相关内容。用于 RAG 问答。参数: 搜索关键词。"""
        try:
            docs = search_vectors(query, top_k=3)
            return json.dumps(docs, ensure_ascii=False, default=str) if docs else "未找到相关知识"
        except Exception as e:
            return f"检索失败: {e}"
    return knowledge_search


# ── Agent → 工具映射 ─────────────────────────────────

_TOOL_FACTORIES: dict[str, list[Callable[[], object]]] = {
    "paper":    [_make_db_tool],
    "question": [_make_db_tool, _make_search_tool],
    "student":  [_make_db_tool],
    "qa":       [_make_db_tool, _make_search_tool],
    "teaching": [_make_search_tool],
}

# 工具缓存（避免重复创建）
_tool_cache: dict[str, list] = {}

def get_tools(agent_name: str) -> list:
    """返回指定 Agent 的 LangChain 工具列表"""
    if agent_name not in _tool_cache:
        factories = _TOOL_FACTORIES.get(agent_name, [])
        _tool_cache[agent_name] = [f() for f in factories]
    return _tool_cache[agent_name]
