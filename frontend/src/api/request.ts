import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器 — 添加 Token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器 — 解包 Result<T>，处理 401 和错误
request.interceptors.response.use(
  response => {
    const { code, message, data } = response.data
    if (code === 200) return data
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  async error => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const res = await axios.post('/api/auth/refresh', {}, {
            headers: { Authorization: `Bearer ${refreshToken}` }
          })
          if (res.data.code === 200) {
            const { accessToken, refreshToken: newRefresh } = res.data.data
            localStorage.setItem('accessToken', accessToken)
            localStorage.setItem('refreshToken', newRefresh)
            error.config.headers.Authorization = `Bearer ${accessToken}`
            return request(error.config)
          }
        } catch { /* ignore */ }
      }
      localStorage.clear()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
      return Promise.resolve(null)
    } else if (error.response?.status === 403) {
      // 权限不足或 token 无效，静默处理
      return Promise.resolve(null)
    } else if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error('网络错误，请稍后重试')
    }
    return Promise.resolve(null)
  }
)

export default request
