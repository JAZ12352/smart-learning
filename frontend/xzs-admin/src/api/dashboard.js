import request from '@/utils/request'

export default {
  index () {
    return request({
      url: '/api/admin/dashboard/index',
      method: 'post'
    })
  }
}
