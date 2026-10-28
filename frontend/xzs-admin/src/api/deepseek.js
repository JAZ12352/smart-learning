import axios from 'axios'

const DEEPSEEK_API_BASE_URL = '/api/admin/deepSeek/api'

const deepSeekApi = axios.create({
  baseURL: DEEPSEEK_API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true,
  timeout: 120000
})

deepSeekApi.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.token = token
    }
    return config
  },
  error => {
    console.error('请求异常：', error)
    return Promise.reject(error)
  }
)

export const generateTitle = async (questionType, gradeLevel, subjectId) => {
  const response = await deepSeekApi.post('/chat', {
    questionType,
    gradeLevel,
    subjectId
  })
  return response.data
}

export const generalChat = async (data) => {
  const response = await deepSeekApi.post('/general-chat', data)
  return response.data
}

export const streamChat = async (data, onChunk) => {
  const token = localStorage.getItem('token') || ''
  const response = await fetch(`${DEEPSEEK_API_BASE_URL}/stream-chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      token
    },
    credentials: 'include',
    body: JSON.stringify(data)
  })

  if (!response.ok) {
    throw new Error('HTTP error! status: ' + response.status)
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
  generateTitle,
  generalChat,
  streamChat
}
