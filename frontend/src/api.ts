import axios from 'axios'
import { ElMessage } from 'element-plus'

/** 统一 axios 实例：baseURL /api（开发期由 Vite 代理到后端 8080） */
const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 统一错误提示：后端业务错误返回 4xx + 中文 message，直接弹出展示
http.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message || '请求失败，请检查网络或稍后重试'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default http
