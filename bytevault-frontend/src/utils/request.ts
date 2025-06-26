import axios from 'axios'
import type { AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const service = axios.create({
  baseURL: '', // 使用vite的proxy配置
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
    const res = response.data
    
    // 根据API返回的状态码处理
    // 假设API返回格式: { code: number, message: string, data: any }
    if (res.code !== 200) {
      ElMessage({
        message: res.message || '操作失败',
        type: 'error',
        duration: 5 * 1000
      })
      
      // 401: 未授权 (token失效)
      if (res.code === 401) {
        // 询问用户是否重新登录
        ElMessage.confirm('您的登录已过期，请重新登录', '提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          // 清除token并跳转到登录页
          localStorage.removeItem('token')
          location.reload()
        })
      }
      
      return Promise.reject(new Error(res.message || '操作失败'))
    } else {
      return res
    }
  },
  (error: AxiosError) => {
    console.error('响应错误:', error)
    
    let message = '连接服务器失败'
    if (error.response) {
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