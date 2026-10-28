const STORAGE_KEY = 'xzs-ai-chat-sessions'

function readSessions () {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const sessions = raw ? JSON.parse(raw) : []
    return Array.isArray(sessions) ? sessions : []
  } catch (error) {
    console.error('读取 AI 对话记录失败：', error)
    return []
  }
}

function writeSessions (sessions) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions))
}

function formatTime (date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

function buildTitle (messages = []) {
  const firstUserMessage = messages.find(item => item.role === 'user' && item.content)
  const content = firstUserMessage ? firstUserMessage.content.trim() : '新对话'
  return content.length > 20 ? `${content.slice(0, 20)}...` : content
}

function upsertSession (session) {
  const sessions = readSessions()
  const index = sessions.findIndex(item => item.id === session.id)
  if (index >= 0) {
    sessions.splice(index, 1, session)
  } else {
    sessions.unshift(session)
  }
  sessions.sort((a, b) => new Date(b.updateTime || b.createTime).getTime() - new Date(a.updateTime || a.createTime).getTime())
  writeSessions(sessions)
  return session
}

function getSessionById (id) {
  return readSessions().find(item => item.id === id) || null
}

async function streamRequest (payload, onChunk) {
  const token = localStorage.getItem('token') || ''
  const response = await fetch('/api/admin/deepSeek/api/stream-chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      token
    },
    credentials: 'include',
    body: JSON.stringify(payload)
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || `HTTP error! status: ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let completeContent = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    const chunk = decoder.decode(value, { stream: true })
    completeContent += chunk
    if (onChunk) {
      onChunk(chunk, completeContent)
    }
  }

  return completeContent
}

export default {
  create (data = {}) {
    const now = formatTime()
    const session = {
      id: data.id || `chat_${Date.now()}`,
      title: data.title || '新对话',
      createTime: now,
      updateTime: now,
      status: 'active',
      messages: Array.isArray(data.messages) ? data.messages : []
    }
    upsertSession(session)
    return Promise.resolve({ code: 200, data: session })
  },

  list () {
    const sessions = readSessions().map(item => ({
      id: item.id,
      title: item.title || '新对话',
      createTime: item.createTime,
      updateTime: item.updateTime || item.createTime,
      status: item.status || 'active'
    }))

    return Promise.resolve({
      code: 200,
      data: {
        list: sessions,
        total: sessions.length
      }
    })
  },

  get (id) {
    const session = getSessionById(id)
    return Promise.resolve({ code: 200, data: session })
  },

  saveSession (data) {
    const existing = getSessionById(data.id)
    const now = formatTime()
    const session = {
      id: data.id,
      title: data.title || buildTitle(data.messages),
      createTime: existing ? existing.createTime : now,
      updateTime: now,
      status: 'active',
      messages: Array.isArray(data.messages) ? data.messages : []
    }
    upsertSession(session)
    return Promise.resolve({ code: 200, data: session })
  },

  async send (data) {
    const response = await fetch('/api/admin/deepSeek/api/general-chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        token: localStorage.getItem('token') || ''
      },
      credentials: 'include',
      body: JSON.stringify({
        message: data.content,
        messages: data.messages
      })
    })

    const result = await response.json()
    if (!response.ok || result.code !== 1) {
      throw new Error(result.message || 'AI 回复失败')
    }

    return {
      code: 200,
      data: {
        answer: result.response.reply
      }
    }
  },

  async sendStream (data, onChunk) {
    const answer = await streamRequest({
      message: data.content,
      messages: data.messages
    }, onChunk)

    return {
      code: 200,
      data: {
        answer
      }
    }
  },

  delete (id) {
    const sessions = readSessions().filter(item => item.id !== id)
    writeSessions(sessions)
    return Promise.resolve({ code: 200 })
  }
}
