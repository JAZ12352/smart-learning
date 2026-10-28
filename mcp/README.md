# 智谱视觉 MCP（DeepSeek Harness 识图）

给 **DeepSeek Harness / DSH** 外挂“眼睛”：

- 平时所有文本对话、代码、Agent 任务都继续用 **DeepSeek**；
- 只有当需要**识图 / 看图 / OCR / 分析截图**时，DSH 里的 DeepSeek 才会调用
  `mcp__zhipu_vision__recognize_image`，把图片交给**智谱 GLM-4V** 识别。

## 文件说明

| 文件 | 作用 |
|---|---|
| `zhipu_vision_mcp.py` | MCP server（stdio，纯 Python 标准库，无额外依赖） |
| `dsh.cordis.patch.yml` | DSH 的 MCP 客户端配置补丁 |
| `dsh-vision-preprocessor/` | DSH 插件：粘贴/上传图片时自动先转成文字 |

## 环境变量

在项目根目录 `.env` 或系统环境变量中配置：

```env
ZHIPU_API_KEY=你的智谱APIKey
ZHIPU_VISION_MODEL=glm-4.6v-flash
ZHIPU_VISION_BASE_URL=https://open.bigmodel.cn/api/paas/v4/chat/completions
```

`ZHIPU_VISION_MODEL` 和 `ZHIPU_VISION_BASE_URL` 可省略，默认值就是上面这两个。

## 给 DSH 永久启用

把 `mcp/dsh.cordis.patch.yml` 里的这段配置追加到 DSH profile 的
`cordis.patch.yml`（`$DSH_HOME` 可通过 `echo $env:DSH_HOME` 查看）：

- Web 模式：`$DSH_HOME\profiles\web\cordis.patch.yml`
- TUI 模式：`$DSH_HOME\profiles\tui\cordis.patch.yml`

示例（Windows，按你的仓库实际路径改 `cwd`）：

```yaml
- insert:
    - id: mcp-zhipu-vision
      name: '@deepseek-ai/dsh-mcp-client'
      config:
        serverName: zhipu_vision
        transport: stdio
        command: python
        args: ['mcp/zhipu_vision_mcp.py']
        cwd: 'C:/Users/20873/Downloads/84015876yuanma'
        env:
          ZHIPU_API_KEY: !!js process.env.ZHIPU_API_KEY || ''
          ZHIPU_VISION_MODEL: !!js process.env.ZHIPU_VISION_MODEL || 'glm-4.6v-flash'
          ZHIPU_VISION_BASE_URL: !!js process.env.ZHIPU_VISION_BASE_URL || 'https://open.bigmodel.cn/api/paas/v4/chat/completions'
        toolCallTimeoutMs: 120000
        failOnStartupError: false
```

或者不永久改配置，每次启动时临时挂载：

```bash
dsh web --patch mcp/dsh.cordis.patch.yml
```

## 手动测试 MCP server

```powershell
# 测试 initialize + tools/list
$lines = @(
  '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}',
  '{"jsonrpc":"2.0","method":"notifications/initialized"}',
  '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'
)
$lines | python mcp/zhipu_vision_mcp.py
```

如果看到 `tools` 里包含 `recognize_image`，说明 server 正常。

## 模型只会“识图时才调用”

工具描述里已经写明：**普通纯文本对话不要调用**。
项目根目录的 `AGENTS.md` 也加了同样的规则，DSH 启动时会自动加载。

## 图片自动预处理（解决“模型不支持图片”）

如果你**直接粘贴/上传图片**，DeepSeek 是纯文本模型，DSH 会在请求发到 DeepSeek 前报
“模型不支持图片”。这个问题不是 MCP 工具能单独解决的，需要先把图片转成文字。

项目里新增了一个 DSH 插件：`mcp/dsh-vision-preprocessor`。

它监听 `agent/pre-step`，在 DeepSeek 收到请求**之前**：

1. 读取用户粘贴/上传的图片附件；
2. 调用智谱 GLM-4.6V-Flash 识别图片；
3. 把图片 block 替换成文字描述 block；
4. 再交给默认 DeepSeek 模型处理。

这样 DeepSeek 永远不会收到 image block，也就不会再报不支持图片。

### 插件安装位置

当前已经复制到你的 DSH profile：

```text
$DSH_HOME\profiles\web\dsh-vision-preprocessor
```

并在 `$DSH_HOME\profiles\web\cordis.patch.yml` 注册：

```yaml
- id: vision-preprocessor
  name: './dsh-vision-preprocessor/index.js'
  config: {}
```

如果以后 DSH profile 重置，重新复制 `mcp/dsh-vision-preprocessor` 到
`$DSH_HOME\profiles\web\` 并加上上面这段配置即可。

## 什么时候用 MCP，什么时候自动预处理

| 场景 | 处理方式 |
|---|---|
| 直接粘贴/上传图片 | 自动被 vision-preprocessor 转成文字，DeepSeek 继续回答 |
| 对话里给图片路径/URL，要求识别 | DeepSeek 调用 `mcp__zhipu_vision__recognize_image` |
| 纯文本对话 | 完全走 DeepSeek，不调用视觉模型 |
