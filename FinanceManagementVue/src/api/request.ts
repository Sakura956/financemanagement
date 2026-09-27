import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
//后端统一返回格式
import type { ApiResponse } from '@/types'

// 扩展 axios 配置
//给 axios 加一个自定义参数：skipErrorToast: true → 不弹出错误提示
declare module 'axios' {
  interface InternalAxiosRequestConfig {
    skipErrorToast?: boolean
  }
}
// 创建 axios 实例
const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },//提交 JSON 格式
})

// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器
instance.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse
    if (body.code === 200) {
      return body.data as any // 直接返回 data，不用每层解包
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message))
  },
  (error: AxiosError<ApiResponse>) => {
    const status = error.response?.status
    const msg = error.response?.data?.message
    const skipToast = (error.config as InternalAxiosRequestConfig)?.skipErrorToast

    if (!skipToast) {
      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        ElMessage.error('登录已过期，请重新登录')
        window.location.href = '/login'
      } else if (status === 403) {
         localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        ElMessage.error(msg || '无权限访问')
        window.location.href = '/login'
      } else if (status === 500) {
        ElMessage.error('服务器内部错误')
      } else if (status === 503) {
        ElMessage.error('AI 服务暂不可用')
      } else {
        ElMessage.error(msg || '网络异常')
      }
    }

    return Promise.reject(error)
  },
)

export default instance
