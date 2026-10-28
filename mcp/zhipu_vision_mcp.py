#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MCP server: 智谱视觉模型 (GLM-4V) 识图工具

给 DeepSeek Harness / DeepSeek 等纯文本模型“装眼睛”。
只有需要识图/看图/OCR 时才由模型调用本 MCP 工具；平时所有对话仍走 DeepSeek。

环境变量:
  ZHIPU_API_KEY         必填，智谱开放平台 API Key
  ZHIPU_VISION_MODEL    可选，默认 glm-4v-flash
  ZHIPU_VISION_BASE_URL 可选，默认 https://open.bigmodel.cn/api/paas/v4/chat/completions

实现: 纯 Python 标准库 + MCP stdio JSON-RPC，无第三方依赖。
"""
from __future__ import annotations

import base64
import json
import mimetypes
import os
import pathlib
import sys
import urllib.error
import urllib.parse
import urllib.request

DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
DEFAULT_MODEL = "glm-4.6v-flash"
PROTOCOL_VERSION = "2025-06-18"

TOOL_NAME = "recognize_image"

TOOL_DEFINITION = {
    "name": TOOL_NAME,
    "description": (
        "识别/理解图片。只有当用户提供了图片路径、图片 URL、data URI，"
        "或要求分析截图、图片、OCR 文字识别、看图回答时，才调用本工具；"
        "普通纯文本对话不要调用。工具会把图片交给智谱视觉模型，返回文字描述/OCR 结果。"
    ),
    "inputSchema": {
        "type": "object",
        "properties": {
            "image": {
                "type": "string",
                "description": "图片位置：本地路径（如 C:/a/b.png）、http(s) URL，或 data:image/...;base64,<data> 数据 URI",
            },
            "prompt": {
                "type": "string",
                "description": "可选。告诉视觉模型识别/回答什么。默认：详细描述图片内容并识别所有可见文字",
            },
        },
        "required": ["image"],
    },
}

DEFAULT_PROMPT = (
    "请详细描述这张图片的内容，并识别其中所有可见文字。"
    "如果图片中有题目、图表、表格、代码或界面，请原样输出关键信息。"
)


def _log(message: str) -> None:
    """日志走 stderr，避免污染 MCP stdio 通道。"""
    sys.stderr.write(f"[zhipu-vision-mcp] {message}\n")
    sys.stderr.flush()


def _guess_mime(path: str) -> str:
    mime, _ = mimetypes.guess_type(path)
    return mime or "image/png"


def _load_image(image_ref: str) -> tuple[str, str]:
    """
    把图片引用转成 (mime_type, base64_data)。
    支持 data URI、http(s) URL、本地文件路径。
    """
    image_ref = image_ref.strip()

    # 1. data URI
    if image_ref.startswith("data:"):
        header, sep, data = image_ref.partition(",")
        if not sep:
            raise ValueError("无效的 data URI")
        mime = header[5:].split(";")[0] or "image/png"
        if ";base64" in header:
            return mime, data
        # 非 base64 data URI：按 URL 编码文本处理
        decoded = urllib.parse.unquote_to_bytes(data)
        return mime, base64.b64encode(decoded).decode("ascii")

    # 2. HTTP(S) URL
    if image_ref.startswith(("http://", "https://")):
        req = urllib.request.Request(image_ref, headers={"User-Agent": "zhipu-vision-mcp/1.0"})
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read()
            mime = resp.headers.get_content_type() or _guess_mime(image_ref)
            return mime, base64.b64encode(raw).decode("ascii")

    # 3. 本地文件
    path = pathlib.Path(image_ref).expanduser()
    if not path.is_file():
        raise FileNotFoundError(f"图片文件不存在: {image_ref}")
    raw = path.read_bytes()
    return _guess_mime(str(path)), base64.b64encode(raw).decode("ascii")


def _call_zhipu_vision(mime: str, b64: str, prompt: str) -> str:
    api_key = os.environ.get("ZHIPU_API_KEY") or os.environ.get("ZHIPUAI_API_KEY")
    if not api_key:
        raise RuntimeError("未设置 ZHIPU_API_KEY，请在 DSH 的 .env / 系统环境变量中配置")

    base_url = os.environ.get("ZHIPU_VISION_BASE_URL", DEFAULT_BASE_URL).rstrip("/")
    model = os.environ.get("ZHIPU_VISION_MODEL", DEFAULT_MODEL)

    payload = {
        "model": model,
        "messages": [
            {
                "role": "user",
                "content": [
                    {
                        "type": "image_url",
                        "image_url": {"url": f"data:{mime};base64,{b64}"},
                    },
                    {"type": "text", "text": prompt},
                ],
            }
        ],
        "temperature": 0.2,
        "stream": False,
    }

    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(
        base_url,
        data=body,
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            result = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="replace")[:500]
        raise RuntimeError(f"智谱 API 返回 {e.code}: {detail}") from e

    try:
        return result["choices"][0]["message"]["content"]
    except (KeyError, IndexError, TypeError) as e:
        raise RuntimeError(f"智谱 API 响应格式异常: {json.dumps(result, ensure_ascii=False)[:500]}") from e


def recognize_image(image: str, prompt: str | None = None) -> str:
    mime, b64 = _load_image(image)
    final_prompt = (prompt or DEFAULT_PROMPT).strip()
    _log(f"recognize_image: mime={mime} prompt_len={len(final_prompt)}")
    return _call_zhipu_vision(mime, b64, final_prompt)


# ────────────────────────── MCP stdio JSON-RPC ──────────────────────────

def _send(msg: dict | None) -> None:
    if msg is None:
        return
    line = json.dumps(msg, ensure_ascii=False) + "\n"
    sys.stdout.buffer.write(line.encode("utf-8"))
    sys.stdout.buffer.flush()


def _read() -> dict | None:
    line = sys.stdin.buffer.readline()
    if not line:
        return None
    line = line.strip()
    if not line:
        return _read()
    try:
        return json.loads(line.decode("utf-8"))
    except json.JSONDecodeError:
        _log(f"忽略无法解析的 JSON: {line[:200]!r}")
        return _read()


def _error(request_id, code: int, message: str) -> dict:
    return {"jsonrpc": "2.0", "id": request_id, "error": {"code": code, "message": message}}


def _handle(message: dict) -> dict | None:
    method = message.get("method", "")
    request_id = message.get("id")

    # 通知类消息没有 id，不回复
    if method == "notifications/initialized":
        return None

    if method == "initialize":
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "result": {
                "protocolVersion": PROTOCOL_VERSION,
                "capabilities": {"tools": {"listChanged": False}},
                "serverInfo": {"name": "zhipu-vision-mcp", "version": "1.0.0"},
            },
        }

    if method == "tools/list":
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "result": {"tools": [TOOL_DEFINITION]},
        }

    if method == "tools/call":
        params = message.get("params", {}) or {}
        name = params.get("name", "")
        arguments = params.get("arguments", {}) or {}

        if name != TOOL_NAME:
            return _error(request_id, -32602, f"未知工具: {name}")

        image = arguments.get("image", "")
        prompt = arguments.get("prompt")
        if not image:
            return {
                "jsonrpc": "2.0",
                "id": request_id,
                "result": {
                    "content": [{"type": "text", "text": "缺少必填参数 image"}],
                    "isError": True,
                },
            }

        try:
            text = recognize_image(image, prompt)
            return {
                "jsonrpc": "2.0",
                "id": request_id,
                "result": {"content": [{"type": "text", "text": text}]},
            }
        except Exception as e:  # noqa: BLE001
            _log(f"recognize_image error: {e}")
            return {
                "jsonrpc": "2.0",
                "id": request_id,
                "result": {
                    "content": [{"type": "text", "text": f"识图失败：{e}"}],
                    "isError": True,
                },
            }

    if method == "ping":
        return {"jsonrpc": "2.0", "id": request_id, "result": {}}

    if method == "shutdown":
        return {"jsonrpc": "2.0", "id": request_id, "result": None}

    if method == "exit":
        sys.exit(0)

    return _error(request_id, -32601, f"不支持的方法: {method}")


def main() -> None:
    _log(f"starting, model={os.environ.get('ZHIPU_VISION_MODEL', DEFAULT_MODEL)}")
    while True:
        message = _read()
        if message is None:
            break
        try:
            response = _handle(message)
        except Exception as e:  # noqa: BLE001
            _log(f"handler error: {e}")
            response = _error(message.get("id"), -32603, str(e))
        _send(response)


if __name__ == "__main__":
    main()
