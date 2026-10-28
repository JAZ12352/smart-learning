import request from '@/utils/request'

const agentLogApi = {
  list: function (params) {
    return request({
      url: '/api/admin/agent-log/list',
      method: 'get',
      params: params
    })
  },
  getByRequestId: function (requestId) {
    return request({
      url: '/api/admin/agent-log/request/' + requestId,
      method: 'get'
    })
  }
}

export default agentLogApi
