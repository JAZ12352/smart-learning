import request from '@/utils/request'

const courseApi = {
  // 获取课程列表
  list: function (params = {}) {
    return request({
      url: '/api/admin/courses',
      method: 'get',
      params: params
    })
  },

  // 获取课程详情
  getById: function (id) {
    return request({
      url: '/api/admin/courses/' + id,
      method: 'get'
    })
  },

  // 获取分类列表
  getCategories: function () {
    return request({
      url: '/api/admin/courses/categories',
      method: 'get'
    })
  },

  // 导入课程
  importCourse: function (courseData) {
    return request({
      url: '/api/admin/courses/import',
      method: 'post',
      data: courseData
    })
  },

  // 从URL导入
  importFromUrl: function (data) {
    return request({
      url: '/api/admin/courses/import-url',
      method: 'post',
      data: data
    })
  },

  // 获取嵌入信息
  getEmbedInfo: function (id) {
    return request({
      url: '/api/admin/courses/' + id + '/embed',
      method: 'get'
    })
  },

  // 删除课程
  delete: function (id) {
    return request({
      url: '/api/admin/courses/' + id,
      method: 'delete'
    })
  },

  // 更新课程
  update: function (id, data) {
    return request({
      url: '/api/admin/courses/' + id,
      method: 'put',
      data: data
    })
  },

  // 上传文件（PPT/PDF/电子课本）
  uploadFile: function (file, category) {
    var formData = new FormData()
    formData.append('file', file)
    if (category) formData.append('category', category)
    return request({
      url: '/api/admin/resource/upload',
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export default courseApi
