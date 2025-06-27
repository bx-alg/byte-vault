import request from '@/utils/request'
import type { UserInfo } from '@/stores/user'

/**
 * 登录响应接口
 */
export interface LoginResponse {
  token: string
  user: UserInfo
}

/**
 * 注册响应接口
 */
export interface RegisterResponse {
  message: string
  username: string
}

/**
 * 用户登录
 * @param username 用户名
 * @param password 密码
 */
export function login(username: string, password: string): Promise<LoginResponse> {
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
 * 用户注册
 * @param username 用户名
 * @param password 密码
 * @param confirmPassword 确认密码
 */
export function register(username: string, password: string, confirmPassword: string): Promise<RegisterResponse> {
  return request({
    url: '/api/auth/register',
    method: 'post',
    data: {
      username,
      password,
      confirmPassword
    }
  })
}

/**
 * 获取当前登录用户信息
 */
export function getUserInfo(): Promise<UserInfo> {
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