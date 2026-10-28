import request from '@/utils/request'

function readStream (response, onChunk) {
  if (!response.ok) {
    throw new Error('HTTP error! status: ' + response.status)
  }
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let completeContent = ''

  function read () {
    return reader.read().then(function (result) {
      if (result.done) return completeContent
      const chunk = decoder.decode(result.value, { stream: true })
      completeContent += chunk
      if (onChunk) {
        onChunk(chunk, completeContent)
      }
      return read()
    })
  }

  return read()
}

const resourceGenerationApi = {
  list: function (params = {}) {
    return request({
      url: '/api/admin/resource/list',
      method: 'get',
      params: params
    })
  },
  getDetail: function (id) {
    return request({
      url: '/api/admin/resource/' + id,
      method: 'get'
    })
  },
  generate: function (data, onChunk) {
    const token = localStorage.getItem('token') || ''
    return fetch('/api/admin/resource/generate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        token
      },
      credentials: 'include',
      body: JSON.stringify(data)
    }).then(function (response) {
      return readStream(response, onChunk)
    })
  },
  delete: function (id) {
    return request({
      url: '/api/admin/resource/' + id,
      method: 'delete'
    })
  },
  evaluate: function (id, onChunk) {
    const token = localStorage.getItem('token') || ''
    return fetch('/api/admin/resource/evaluate/' + id, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        token
      },
      credentials: 'include'
    }).then(function (response) {
      return readStream(response, onChunk)
    })
  },
  // Agnes 2.0 视频生成
  generateVideoAgnes: function (data) {
    return request({
      url: '/api/admin/resource/generate-video-agnes',
      method: 'post',
      data: data
    })
  },
  queryVideoStatusAgnes: function (taskId) {
    return request({
      url: '/api/admin/resource/video-status-agnes',
      method: 'get',
      params: { taskId: taskId }
    })
  },
  // Agnes Image 2.1 Flash 思维导图图片生成
  generateMindmapImage: function (data) {
    return request({
      url: '/api/admin/resource/generate-mindmap-image',
      method: 'post',
      data: data
    })
  },
  queryMindmapImageStatus: function (taskId) {
    return request({
      url: '/api/admin/resource/mindmap-image-status',
      method: 'get',
      params: { taskId: taskId }
    })
  },
  // 下载远程资源到本地
  proxyResource: function (url, fileName) {
    return request({
      url: '/api/admin/resource/proxy',
      method: 'post',
      data: { url: url, fileName: fileName || '' }
    })
  },
  // 搜索PPT配图
  searchImages: function (keyword, count) {
    return request({
      url: '/api/admin/resource/search-images',
      method: 'post',
      data: { keyword: keyword, count: count || 3 }
    })
  },
  // 获取已下载资源列表
  listDownloads: function () {
    return request({
      url: '/api/admin/resource/downloads',
      method: 'get'
    })
  },
  // ═══════════════════════════════════════════════════════════════
  //  多智能体编排 API
  // ═══════════════════════════════════════════════════════════════

  /** 批量多智能体生成（所有Agent串行） */
  multiAgentGenerate: function (data, onChunk) {
    const token = localStorage.getItem('token') || ''
    return fetch('/api/admin/resource/multi-agent-generate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        token
      },
      credentials: 'include',
      body: JSON.stringify(data)
    }).then(function (response) {
      return readStream(response, onChunk)
    })
  },

  /** 获取推荐的智能体列表 */
  recommendAgents: function (topic) {
    return request({
      url: '/api/admin/resource/recommend-agents',
      method: 'post',
      data: { topic: topic }
    })
  },

  /** 单个Agent生成（带agentType路由） */
  agentGenerate: function (data, onChunk) {
    const token = localStorage.getItem('token') || ''
    return fetch('/api/admin/resource/agent-generate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        token
      },
      credentials: 'include',
      body: JSON.stringify(data)
    }).then(function (response) {
      return readStream(response, onChunk)
    })
  }
}

export default resourceGenerationApi
