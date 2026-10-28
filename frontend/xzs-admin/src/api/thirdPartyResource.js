import request from '@/utils/request'

const thirdPartyApi = {
  // 获取支持的平台列表
  getPlatforms () {
    return request({
      url: '/api/admin/third-party/platforms',
      method: 'get'
    })
  },

  // 识别URL归属平台
  detectPlatform (url) {
    return request({
      url: '/api/admin/third-party/detect-platform',
      method: 'post',
      data: { url }
    })
  },

  // 搜索第三方资源
  search (keyword, platform, resourceType) {
    return request({
      url: '/api/admin/third-party/search',
      method: 'post',
      data: {
        keyword: keyword || '',
        platform: platform || '',
        resourceType: resourceType || ''
      }
    })
  },

  // 获取搜索建议
  getSuggestions (keyword, platform) {
    return request({
      url: '/api/admin/third-party/suggestions',
      method: 'get',
      params: { keyword: keyword || '', platform: platform || '' }
    })
  },

  // 获取单个资源
  fetchResource (url, forceRefresh) {
    return request({
      url: '/api/admin/third-party/fetch',
      method: 'post',
      data: { url: url, forceRefresh: forceRefresh || false }
    })
  },

  // 批量获取资源
  fetchBatch (urls, forceRefresh) {
    return request({
      url: '/api/admin/third-party/fetch-batch',
      method: 'post',
      data: { urls: urls || [], forceRefresh: forceRefresh || false }
    })
  },

  // 获取缓存列表
  listCached () {
    return request({
      url: '/api/admin/third-party/cached',
      method: 'get'
    })
  },

  // 删除缓存资源
  deleteCached (fileName) {
    return request({
      url: '/api/admin/third-party/cached/' + encodeURIComponent(fileName),
      method: 'delete'
    })
  },

  // 清空所有缓存
  clearAllCached () {
    return request({
      url: '/api/admin/third-party/cached',
      method: 'delete'
    })
  }
}

export default thirdPartyApi
