/**
 * dsh-vision-preprocessor
 *
 * 在 DSH agent/pre-step 阶段自动拦截用户消息里的图片附件：
 *   1. 用智谱 GLM-4.6V-Flash 识别图片，转成文字描述；
 *   2. 把图片 block 替换成文字 block；
 *   3. 再交给默认 DeepSeek 模型处理。
 *
 * 这样 DeepSeek 不会收到 image block，也就不会再报“模型不支持图片”。
 * 普通纯文本对话完全不受影响，仍然只走 DeepSeek。
 */

export const name = 'dsh-vision-preprocessor'
export const inject = ['attachments']

const DEFAULT_MODEL = 'glm-4.6v-flash'
const DEFAULT_BASE_URL = 'https://open.bigmodel.cn/api/paas/v4/chat/completions'
const DEFAULT_PROMPT =
  '请详细描述这张图片的内容，并识别其中所有可见文字。' +
  '如果图片中有题目、图表、表格、代码或界面，请原样输出关键信息。'

function hasImageBlock(message) {
  return Array.isArray(message?.content) && message.content.some((block) => block?.type === 'image')
}

function getApiKey() {
  return process.env.ZHIPU_API_KEY || process.env.ZHIPUAI_API_KEY || ''
}

async function describeImage(ctx, attachment, userText, signal) {
  const apiKey = getApiKey()
  if (!apiKey) {
    throw new Error('未设置 ZHIPU_API_KEY，请在 .env / 系统环境变量中配置')
  }

  const stored = await ctx.attachments.readImage(attachment, signal)
  const dataUrl = `data:${attachment.mediaType};base64,${Buffer.from(stored.data).toString('base64')}`
  const prompt = (userText || DEFAULT_PROMPT).trim()
  const model = process.env.ZHIPU_VISION_MODEL || DEFAULT_MODEL
  const baseUrl = (process.env.ZHIPU_VISION_BASE_URL || DEFAULT_BASE_URL).replace(/\/+$/, '')

  const response = await fetch(baseUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${apiKey}`,
    },
    body: JSON.stringify({
      model,
      messages: [
        {
          role: 'user',
          content: [
            { type: 'text', text: prompt },
            { type: 'image_url', image_url: { url: dataUrl } },
          ],
        },
      ],
      temperature: 0.2,
      stream: false,
    }),
    signal,
  })

  const raw = await response.text()
  if (!response.ok) {
    throw new Error(`智谱 API 返回 ${response.status}: ${raw.slice(0, 500)}`)
  }

  const data = JSON.parse(raw)
  const content = data?.choices?.[0]?.message?.content
  if (!content) {
    throw new Error(`智谱 API 返回为空: ${raw.slice(0, 500)}`)
  }
  return String(content).trim()
}

async function rewriteMessage(ctx, message, signal) {
  const textParts = message.content
    .filter((block) => block?.type === 'text' && typeof block.text === 'string')
    .map((block) => block.text)
  const userText = textParts.join('\n').trim()

  const newContent = []
  for (const block of message.content) {
    if (block?.type !== 'image') {
      newContent.push(block)
      continue
    }
    const description = await describeImage(ctx, block.attachment, userText, signal)
    newContent.push({
      type: 'text',
      text: `【用户上传图片的自动识别结果】\n${description}`,
    })
  }

  return { ...message, content: newContent }
}

export function apply(ctx) {
  ctx.on('agent/pre-step', async (payload, next) => {
    const downstream = await next()
    if (downstream?.kind !== 'enter') return downstream

    const messages = downstream.messages
    if (!messages.some(hasImageBlock)) return downstream

    try {
      const rewritten = await Promise.all(
        messages.map((message) => hasImageBlock(message)
          ? rewriteMessage(ctx, message, payload.signal)
          : message),
      )
      return { kind: 'enter', messages: rewritten }
    } catch (error) {
      ctx.logger?.warn?.('dsh-vision-preprocessor: %o', error)
      const fallbackText =
        '[图片自动识别失败，请检查 ZHIPU_API_KEY 后重试，或手动要求模型调用 mcp__zhipu_vision__recognize_image]\n' +
        `${error?.message || String(error)}`
      const fallbackMessages = messages.map((message) => {
        if (!hasImageBlock(message)) return message
        return {
          ...message,
          content: [
            { type: 'text', text: fallbackText },
            ...message.content.filter((block) => block.type !== 'image'),
          ],
        }
      })
      return { kind: 'enter', messages: fallbackMessages }
    }
  })
}
