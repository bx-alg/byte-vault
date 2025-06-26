import request from '@/utils/request'

/**
 * 用户登录
 * @param username 用户名
 * @param password 密码
 */
export function login(username: string, password: string) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data: {
      username,
      password
    }
  })
}

/**
 * 获取当前登录用户信息
 */
export function getUserInfo() {
  return request({
    url: '/api/auth/info',
    method: 'get'
  })
}

/**
 * 用户登出
 */
export function logout() {
  return request({
    url: '/api/auth/logout',
    method: 'post'
  })
} 