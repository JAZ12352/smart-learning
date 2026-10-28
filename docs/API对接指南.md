# API 对接指南

## 概述

智能教师管理系统提供 RESTful API 接口，支持外部系统集成。所有 API 返回统一 JSON 格式。

### 基础信息

| 项目 | 说明 |
|------|------|
| 协议 | HTTP/HTTPS |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| 认证方式 | Bearer Token（登录后获取） |
| API 文档 | `http://{host}:8001/swagger-ui/index.html` |

## 统一返回格式

```json
{
  "code": 200,
  "message": "success",
  "response": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | Integer | 状态码，200 表示成功 |
| `message` | String | 提示信息 |
| `response` | Object/Array | 响应数据 |

## 认证流程

### 1. 获取公钥

```
GET /api/admin/upload/configAndUpload
```

返回 RSA 公钥，用于加密登录密码。

### 2. 登录

```
POST /api/user/login
Content-Type: application/json

{
  "userName": "admin",
  "password": "<RSA加密后的密码>",
  "remember": true
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "response": {
    "token": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "userName": "admin",
    "role": 3
  }
}
```

### 3. 后续请求

在请求头中添加 Token：

```
Authorization: Bearer xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

Token 有效期 12 小时，过期需重新登录。

## 核心 API 概览

### 学生画像

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/student-profile/{userId}` | 获取学生画像 |
| POST | `/api/admin/student-profile/{userId}/update` | 更新画像 |
| POST | `/api/admin/student-profile/{userId}/dialogue` | AI 对话式画像构建（SSE 流式） |
| POST | `/api/admin/student-profile/{userId}/explain` | AI 解读画像（SSE 流式） |

### 学习路径

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/learning-path/{userId}/{subjectId}` | 获取学习路径 |
| POST | `/api/admin/learning-path/generate` | AI 生成个性化路径（SSE 流式） |
| POST | `/api/admin/learning-path/progress` | 更新学习进度 |

### 知识库

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/knowledge-base/list` | 知识库列表 |
| POST | `/api/admin/knowledge-base/create` | 创建知识库 |
| POST | `/api/admin/knowledge-base/upload` | 上传文档（multipart） |
| POST | `/api/admin/knowledge-base/search` | 搜索知识库 |
| POST | `/api/admin/knowledge-base/rag-chat` | RAG 问答（SSE 流式） |

### 资源生成

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/resource/generate` | 单个资源生成 |
| POST | `/api/admin/resource/multi-agent-generate` | 多 Agent 协作生成 |
| POST | `/api/admin/resource/recommend-agents` | 推荐 Agent |
| POST | `/api/admin/resource/generate-pptx` | 生成 PPTX 文件 |
| POST | `/api/admin/resource/evaluate` | 评估资源质量 |

### 学习评估

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/evaluation/progress/{userId}` | 学习进度概览 |
| POST | `/api/admin/evaluation/report/{userId}` | AI 生成评估报告（SSE 流式） |

### 教学意图分析

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/teaching-intent/analyze` | 分析教学意图（SSE 流式） |
| POST | `/api/admin/teaching-intent/generate-ppt` | 生成 PPT 内容（SSE 流式） |
| POST | `/api/admin/teaching-intent/generate-word` | 生成 Word 教案（SSE 流式） |

### 模型配置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/model-config` | 获取当前模型配置 |
| POST | `/api/admin/model-config/test` | 测试 API 连接 |

### AI 对话

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/deepSeek/api/chat` | 非流式 AI 对话 |
| POST | `/api/admin/deepSeek/api/stream-chat` | 流式 AI 对话（SSE） |
| POST | `/api/admin/deepSeek/api/general-chat` | 通用聊天 |

## SSE 流式响应格式

流式 API 使用 Server-Sent Events 协议：

```
data: {"content": "文本片段", "done": false}

data: {"content": "", "done": true}
```

客户端示例（JavaScript）：

```javascript
const response = await fetch('/api/admin/deepSeek/api/stream-chat', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
  body: JSON.stringify({ messages: [{ role: 'user', content: '你好' }] })
});

const reader = response.body.getReader();
const decoder = new TextDecoder();
while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  const text = decoder.decode(value);
  // 解析 SSE 格式: "data: {...}\n\n"
  console.log(text);
}
```

## 错误码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证或 Token 过期 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 在线 API 文档

启动后端后，访问 Swagger UI 获取交互式 API 文档：

- **Swagger UI:** `http://localhost:8001/swagger-ui/index.html`
- **OpenAPI JSON:** `http://localhost:8001/v3/api-docs`

Swagger UI 支持在线调试：填写认证 Token 后可直接在浏览器中调用所有 API 接口。
