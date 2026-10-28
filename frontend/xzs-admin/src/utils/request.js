import axios from 'axios'
import { Message } from 'element-ui'

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 120000
})

service.interceptors.request.use(
  config => {
    config.withCredentials = true
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.token = token
    }
    return config
  },
  error => {
    console.log('请求异常：', error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (typeof res !== 'object') {
      Message.error('接口响应格式错误')
      return Promise.reject(new Error('响应格式错误'))
    }

    if (res.code === 401) {
      Message.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      window.location.href = '/#/login'
      return Promise.reject(new Error('未登录'))
    }

    return res
  },
  error => {
    console.log('响应异常：', error)
    const errMsg = error.message || '接口请求失败'
    Message.error(errMsg)
    return Promise.reject(error)
  }
)

export function post (url, data, config) {
  return service.post(url, data, config)
}

export function postWithLoadTip (url, data) {
  return service.post(url, data)
}

export function get (url, params) {
  return service.get(url, { params })
}

export function del (url) {
  return service.delete(url)
}

export default service
