const https = require('https')
const http = require('http')
const readline = require('readline')

const CONFIG = {
  apiKey: process.env.DEEPSEEK_API_KEY || '',
  apiUrl: 'https://api.deepseek.com/chat/completions',
  models: {
    supervisor: { model: 'deepseek-v4-pro', temperature: 0.3 },
    paper: { model: 'deepseek-v4-pro', temperature: 0.5 },
    question: { model: 'deepseek-v4-pro', temperature: 0.5 },
    profile: { model: 'deepseek-v4-pro', temperature: 0.5 },
    resource: { model: 'deepseek-v4-pro', temperature: 0.5 },
    path: { model: 'deepseek-v4-pro', temperature: 0.4 },
    qa: { model: 'deepseek-v4-pro', temperature: 0.7 },
    teaching: { model: 'deepseek-v4-pro', temperature: 0.7 }
  }
}

const AGENT_PROMPTS = {
  supervisor: '你是 Supervisor Agent，负责意图识别、任务路由和结果汇总。把请求归类到 paper_generate / question_recommend / student_analysis / profile_build / resource_generate / learning_path / qa / teaching / general_chat。只返回 JSON。',
  paper: '你是 Paper Agent，负责智能组卷。输出结构化试卷 JSON。',
  question: '你是 Question Agent，负责练习推荐和题库生成。输出推荐理由和题目 JSON。',
  profile: '你是 Profile Agent，负责对话式学习画像构建，覆盖专业、年级、知识基础、学习风格、目标、薄弱点等维度。',
  resource: '你是 Resource Agent，负责文档、思维导图、题目、拓展阅读、视频动画、代码案例等多模态资源生成。',
  path: '你是 Learning Path Agent，负责根据画像生成个性化学习路径。',
  qa: '你是 QA Agent，负责智能辅导。给思路、例子和下一步练习。',
  teaching: '你是 Teaching Agent，负责教案、课堂活动和 PPT 大纲。'
}

function callLLM (agentName, messages) {
  return new Promise((resolve, reject) => {
    const cfg = CONFIG.models[agentName] || CONFIG.models.supervisor
    const body = JSON.stringify({
      model: cfg.model,
      messages,
      temperature: cfg.temperature,
      max_tokens: 4096
    })

    const url = new URL(CONFIG.apiUrl)
    const req = https.request({
      hostname: url.hostname,
      port: 443,
      path: url.pathname,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${CONFIG.apiKey}`
      }
    }, res => {
      let data = ''
      res.on('data', chunk => { data += chunk })
      res.on('end', () => {
        try {
          const json = JSON.parse(data)
          if (json.error) return reject(new Error(json.error.message))
          resolve(json.choices[0].message.content)
        } catch (e) {
          reject(new Error(`Parse error: ${data.slice(0, 200)}`))
        }
      })
    })
    req.on('error', reject)
    req.write(body)
    req.end()
  })
}

function extractJSON (text) {
  try { return JSON.parse(text) } catch (e) {}
  const match = text.match(/```(?:json)?\s*([\s\S]*?)```/) || text.match(/(\{[\s\S]*\})/)
  if (match) {
    try { return JSON.parse(match[1]) } catch (e) {}
  }
  return {}
}

function routeAgent (intent) {
  return {
    paper_generate: 'paper',
    question_recommend: 'question',
    student_analysis: 'profile',
    profile_build: 'profile',
    resource_generate: 'resource',
    learning_path: 'path',
    qa: 'qa',
    teaching: 'teaching',
    general_chat: 'supervisor'
  }[intent] || 'supervisor'
}

async function runWorkflow (userMessage, userId, userRole, context) {
  const raw = await callLLM('supervisor', [
    { role: 'system', content: AGENT_PROMPTS.supervisor },
    { role: 'user', content: `用户消息：${userMessage}\n角色：${userRole}\n上下文：${JSON.stringify(context || {})}` }
  ])
  const classified = extractJSON(raw)
  const intent = classified.intent || 'general_chat'
  const agent = routeAgent(intent)

  const content = await callLLM(agent, [
    { role: 'system', content: AGENT_PROMPTS[agent] || AGENT_PROMPTS.supervisor },
    { role: 'user', content: `请求：${userMessage}\n用户ID：${userId || ''}\n上下文：${JSON.stringify(context || {})}` }
  ])

  return {
    intent,
    complexity: classified.complexity || 'simple',
    mode: classified.mode || 'route',
    agentResults: [{ agent, content }],
    reply: content
  }
}

function startServer (port = 8100) {
  const server = http.createServer((req, res) => {
    res.setHeader('Content-Type', 'application/json; charset=utf-8')
    res.setHeader('Access-Control-Allow-Origin', '*')
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type')

    if (req.method === 'OPTIONS') {
      res.writeHead(204)
      res.end()
      return
    }

    if (req.url === '/api/v1/health') {
      res.writeHead(200)
      res.end(JSON.stringify({ status: 'ok', version: '2.0.0' }))
      return
    }

    if (req.method === 'POST' && req.url === '/api/v1/chat') {
      let body = ''
      req.on('data', chunk => { body += chunk })
      req.on('end', async () => {
        try {
          const { message, user_id, user_role, context } = JSON.parse(body)
          const state = await runWorkflow(message, user_id, user_role || 'student', context)
          res.writeHead(200)
          res.end(JSON.stringify({
            intent: state.intent,
            reply: state.reply,
            metadata: { complexity: state.complexity, mode: state.mode },
            sub_agent_results: state.agentResults
          }, null, 2))
        } catch (e) {
          res.writeHead(500)
          res.end(JSON.stringify({ error: e.message }))
        }
      })
      return
    }

    res.writeHead(404)
    res.end(JSON.stringify({ error: 'Not found' }))
  })

  server.listen(port, () => {
    console.log(`Multi-Agent service started: http://localhost:${port}`)
  })
  return server
}

module.exports = { runWorkflow, startServer }

if (process.argv.includes('--server')) {
  const idx = process.argv.indexOf('--port')
  const port = idx >= 0 ? parseInt(process.argv[idx + 1]) : 8100
  startServer(port || 8100)
} else {
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout })
  console.log('Multi-Agent interactive mode. Type "exit" to quit, "server" to start API.')

  async function ask () {
    rl.question('> ', async msg => {
      if (!msg.trim()) return ask()
      if (msg === 'exit') {
        rl.close()
        return
      }
      if (msg === 'server') {
        startServer()
        return ask()
      }
      try {
        const state = await runWorkflow(msg, 1, 'student', {})
        console.log('\n' + state.reply + '\n')
      } catch (e) {
        console.error('Error:', e.message)
      }
      ask()
    })
  }
  ask()
}
