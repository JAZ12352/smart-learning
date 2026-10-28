import request from '@/utils/request'

export default {
  getConfig () {
    return request.get('/api/admin/model-config')
  },
  updateConfig (data) {
    return request.post('/api/admin/model-config', data)
  },
  testConnection () {
    return request.post('/api/admin/model-config/test')
  },
  testVideoConnection () {
    return request.post('/api/admin/model-config/test?type=video')
  }
}
