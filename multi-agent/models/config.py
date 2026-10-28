from __future__ import annotations

import os
import re
from pathlib import Path
from typing import Any

import yaml


ENV_PATTERN = re.compile(r"\$\{([A-Za-z_][A-Za-z0-9_]*)(?::([^}]*))?\}|\$([A-Za-z_][A-Za-z0-9_]*)")


def _resolve_env(value: str) -> str:
    def replacer(match: re.Match) -> str:
        name = match.group(1) or match.group(3)
        default = match.group(2) or ""
        return os.environ.get(name, default)

    return ENV_PATTERN.sub(replacer, value)


def _walk(obj: Any) -> Any:
    if isinstance(obj, str):
        return _resolve_env(obj)
    if isinstance(obj, dict):
        return {k: _walk(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [_walk(v) for v in obj]
    return obj


class AppConfig:
    """Load config.yaml and resolve ${ENV:default} placeholders."""

    def __init__(self, path: str | None = None) -> None:
        if path is None:
            path = Path(__file__).resolve().parent.parent / "config.yaml"
        with open(path, encoding="utf-8") as fh:
            raw: dict[str, Any] = yaml.safe_load(fh)
        self._data: dict[str, Any] = _walk(raw)

    @property
    def llm_backends(self) -> dict[str, Any]:
        return self._data.get("llm_backends", {})

    @property
    def agent_routing(self) -> dict[str, Any]:
        return self._data.get("agent_routing", {})

    @property
    def java_backend(self) -> dict[str, Any]:
        return self._data.get("java_backend", {})

    @property
    def mysql_readonly(self) -> dict[str, Any]:
        return self._data.get("mysql_readonly", {})

    @property
    def chromadb(self) -> dict[str, Any]:
        return self._data.get("chromadb", {})

    @property
    def server(self) -> dict[str, Any]:
        return self._data.get("server", {})

    def get_agent_cfg(self, name: str) -> dict[str, Any]:
        return self.agent_routing.get(name, {})

    def get_backend_cfg(self, name: str) -> dict[str, Any]:
        return self.llm_backends.get(name, {})


config = AppConfig()
