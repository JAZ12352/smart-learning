import request from '@/utils/request'

/**
 * 考勤管理模块接口
 * 后端路径: /api/admin/attendance (使用 @RequestParam, POST 方法)
 */
const BASE_URL = '/api/admin/attendance'

export default {
  /**
   * 分页查询考勤记录
   */
  page (queryParam) {
    return request({
      url: `${BASE_URL}/page`,
      method: 'post',
      params: queryParam
    })
  },

  /**
   * 新增考勤记录
   */
  add (data) {
    return request({
      url: `${BASE_URL}/add`,
      method: 'post',
      data: data
    })
  },

  /**
   * 更新考勤记录
   */
  update (data) {
    return request({
      url: `${BASE_URL}/update`,
      method: 'put',
      data: data
    })
  },

  /**
   * 删除考勤记录
   */
  delete (id) {
    return request({
      url: `${BASE_URL}/${id}`,
      method: 'delete'
    })
  },

  /** 今日签到 */
  checkToday () {
    return request({
      url: `${BASE_URL}/check-today`,
      method: 'post'
    })
  },

  /**
   * 请假列表
   */
  getLeaveList (queryParam) {
    return request({
      url: `${BASE_URL}/leave/page`,
      method: 'post',
      params: queryParam
    })
  },

  /** 申请请假 */
  applyLeave (data) {
    return request({
      url: `${BASE_URL}/leave/apply`,
      method: 'post',
      data: data
    })
  },

  /**
   * 审批请假
   */
  approveLeave (data) {
    return request({
      url: `${BASE_URL}/leave/approve`,
      method: 'post',
      data: data
    })
  },

  /**
   * 出勤统计
   */
  getStatistics (queryParam) {
    return request({
      url: `${BASE_URL}/statistics`,
      method: 'post',
      params: queryParam
    })
  },

  /**
   * 缺勤预警
   */
  getWarningList (queryParam) {
    return request({
      url: `${BASE_URL}/warning`,
      method: 'post',
      params: queryParam
    })
  }
}
