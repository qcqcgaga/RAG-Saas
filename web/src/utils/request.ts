import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { message } from 'ant-design-vue'
import router from '@/router'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

const request: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器 — 注入 Token
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 — 统一处理错误
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response
    if (data.code !== 0) {
      // 业务错误码处理
      if (data.code === 40100) {
        // 未认证 — token 无效或过期
        localStorage.removeItem('token')
        localStorage.removeItem('role')
        localStorage.removeItem('tenantId')
        router.push({ name: 'login' })
        message.error('登录已过期，请重新登录')
      } else if (data.code === 40300) {
        // 无权限
        message.error('无操作权限')
      } else {
        message.error(data.msg || '请求失败')
      }
      return Promise.reject(new Error(data.msg))
    }
    return data
  },
  (error) => {
    const status = error.response?.status
    const data = error.response?.data

    if (status === 401) {
      // Spring Security 未认证
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('tenantId')
      router.push({ name: 'login' })
      message.error('登录已过期，请重新登录')
    } else if (status === 403) {
      // Spring Security 无权限
      message.error(data?.msg || '无操作权限')
    } else if (status === 404) {
      // 资源不存在（包括API路径错误）
      message.error(data?.msg || '请求的接口不存在')
    } else {
      message.error(data?.msg || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
