import axios from 'axios'
import type { AxiosResponse, AxiosError } from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const service = axios.create({
  baseURL: '', // 不设置baseURL，使用相对路径
  timeout: 15000
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    
    // 如果有token就在请求头中添加
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    // 检查是否有新的token在响应头中（自动续期）
    const newToken = response.headers['authorization']
    if (newToken && newToken.startsWith('Bearer ')) {
      const token = newToken.substring(7)
      localStorage.setItem('token', token)
      console.log('令牌已自动刷新')
    }
    
    // 直接返回数据
    return response.data
  },
  (error: AxiosError) => {
    console.error('响应错误:', error)
    
    let message = '连接服务器失败'
    
    if (error.code === 'ECONNABORTED') {
      message = '请求超时，请检查网络连接'
    } else if (error.message && error.message.includes('Network Error')) {
      message = '网络错误，请检查您的网络连接'
    } else if (error.response) {
      const status = error.response.status
      
      switch (status) {
        case 400:
          message = '请求错误'
          break
        case 401:
          message = '未授权，请重新登录'
          // 清除token并跳转到登录页
          localStorage.removeItem('token')
          location.reload()
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求地址错误'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = `连接错误 ${status}`
      }
    }
    
    ElMessage({
      message,
      type: 'error',
      duration: 5 * 1000
    })
    
    return Promise.reject(error)
  }
)

export default service