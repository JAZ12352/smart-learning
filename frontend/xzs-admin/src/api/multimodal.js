import request from '@/utils/request'

export const generateLessonPlan = (formData) => {
  return request({
    url: '/api/admin/deepSeek/api/page',
    method: 'post',
    data: formData
  })
}

export const generatePPT = (data) => {
  return request({
    url: '/api/admin/teaching-intent/generate-ppt',
    method: 'post',
    data: data
  })
}

export const generateWordLessonPlan = (data) => {
  return request({
    url: '/api/admin/teaching-intent/generate-word',
    method: 'post',
    data: data
  })
}

export const generateContent = (data) => {
  return request({
    url: '/api/admin/teaching-intent/generate-content',
    method: 'post',
    data: data
  })
}
