import axios from 'axios'

const API_BASE_URL = '/api/admin/knowledge-base'

const knowledgeBaseApi = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  },
  timeout: 180000
})

// 添加请求拦截器，为所有请求添加认证头
knowledgeBaseApi.interceptors.request.use(
  config => {
    // 设置 withCredentials 为 true，确保携带 cookie
    config.withCredentials = true

    // 从localStorage获取token并添加到请求头
    let token = localStorage.getItem('token')
    // 如果 localStorage 中没有 token，尝试从 cookie 中获取
    if (!token) {
      const cookieMatch = document.cookie.match(/xzs=([^;]+)/)
      if (cookieMatch) {
        token = cookieMatch[1]
        localStorage.setItem('token', token)
      }
    }
    if (token) {
      config.headers['token'] = token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 添加响应拦截器，处理认证错误
knowledgeBaseApi.interceptors.response.use(
  response => {
    const res = response.data
    // 处理401未登录错误
    if (res && res.code === 401) {
      console.error('知识库API认证失败：', res.message)
      // 跳转到登录页面
      window.location.href = '/#/login'
      return Promise.reject(new Error(res.message || '未登录'))
    }
    return response
  },
  error => {
    console.error('知识库API请求异常：', error)
    return Promise.reject(error)
  }
)

// 删除知识库（别名）
knowledgeBaseApi.delete = (id) => {
  return knowledgeBaseApi.deleteKB(id)
}

// RAG查询（简化接口）
knowledgeBaseApi.ragQuery = async (kbId, question, onChunk) => {
  var full = ''
  var refs = null
  return knowledgeBaseApi.ragChat(
    { kbId: kbId, message: question },
    function (chunk, references) {
      full += chunk
      if (references) refs = references
      if (onChunk) onChunk(chunk, full, refs)
    }
  )
}

// 获取知识库列表
knowledgeBaseApi.getList = () => {
  return knowledgeBaseApi.get('/list').then(res => res.data)
}

// 创建知识库
knowledgeBaseApi.create = (data) => {
  return knowledgeBaseApi.post('/create', data).then(res => res.data)
}

// 删除知识库
knowledgeBaseApi.deleteKB = (id) => {
  return knowledgeBaseApi.delete(`/${id}`).then(res => res.data)
}

// 获取文档内容
knowledgeBaseApi.getDocumentContent = (id) => {
  return knowledgeBaseApi.get('/document/' + id + '/content').then(res => res.data)
}
// 获取文档列表
knowledgeBaseApi.getDocuments = (kbId) => {
  return knowledgeBaseApi.get(`/${kbId}/documents`).then(res => res.data)
}

// 删除文档
knowledgeBaseApi.deleteDocument = (docId) => {
  return knowledgeBaseApi.delete(`/document/${docId}`).then(res => res.data)
}

// 重新处理文档
knowledgeBaseApi.reprocessDocument = (docId) => {
  return knowledgeBaseApi.post(`/document/${docId}/reprocess`).then(res => res.data)
}

// 向量检索
knowledgeBaseApi.search = (params) => {
  return knowledgeBaseApi.post('/search', params).then(res => res.data)
}

// RAG问答（流式）
knowledgeBaseApi.ragChat = async (data, onChunk) => {
  const token = localStorage.getItem('token') || ''

  try {
    const response = await fetch(`${API_BASE_URL}/rag-chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'token': token
      },
      credentials: 'include',
      body: JSON.stringify(data)
    })

    if (!response.ok) {
      const errorText = await response.text()
      let errorMsg = `HTTP error! status: ${response.status}`
      try {
        const errorJson = JSON.parse(errorText)
        if (errorJson.error) errorMsg = errorJson.error
      } catch (e) {
        // 非JSON错误
      }
      throw new Error(errorMsg)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    let references = null

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() // 保留不完整的行

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) continue
        try {
          const json = JSON.parse(trimmed)
          if (json.chunk) {
            // 保存 references（只在第一个包中发送）
            if (json.references && !references) {
              references = json.references
            }
            onChunk(json.chunk, references)
          }
          if (json.error) {
            onChunk(`[错误] ${json.error}`, references)
          }
        } catch (e) {
          // 尝试作为纯文本处理
          if (trimmed.length > 0) {
            onChunk(trimmed, references)
          }
        }
      }
    }

    // 处理剩余数据
    if (buffer.trim()) {
      try {
        const json = JSON.parse(buffer.trim())
        if (json.chunk) {
          if (json.references && !references) {
            references = json.references
          }
          onChunk(json.chunk, references)
        }
      } catch (e) {
        // 忽略解析错误
      }
    }

    return { code: 1, message: 'success' }
  } catch (error) {
    console.error('RAG chat error:', error)
    throw error
  }
}

export {
  knowledgeBaseApi
}
