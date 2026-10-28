import request from '@/utils/request'

const teachingIntentApi = {
  // 分析教学意图
  analyze: function (data) {
    return request({
      url: '/api/admin/teaching-intent/analyze',
      method: 'post',
      data: data
    })
  }
}

export default teachingIntentApi
