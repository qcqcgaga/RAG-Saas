import axios from 'axios'
import type { R } from '@/types/api'

/**
 * Axios 实例 — 全局请求/响应拦截
 *
 * - 请求拦截：自动附加 Authorization Token
 * - 响应拦截：统一错误处理、401 跳转登录
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const data = response.data as R<unknown>
    if (data.code !== 0) {
      // 业务错误 — 使用 Ant Design message 提示
      console.error(`[${data.code}] ${data.msg}`)
      return Promise.reject(new Error(data.msg))
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

export default request
