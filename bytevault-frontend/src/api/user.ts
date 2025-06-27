import request from '@/utils/request'
import type { UserInfo } from '@/stores/user'

/**
 * 检查用户登录状态
 */
export function checkLoginStatus(): Promise<UserInfo> {
  return request({
    url: '/api/auth/info',
    method: 'get'
  })
}

/**
 * 获取用户列表（仅管理员）
 */
export function getUserList() {
  return request({
    url: '/api/admin/users',
    method: 'get'
  })
}

/**
 * 封禁用户（仅管理员）
 * @param userId 用户ID
 * @param minutes 封禁时间（分钟）
 * @param reason 封禁原因
 */
export function banUser(userId: number, minutes: number = 60, reason: string = '违反用户协议') {
  return request({
    url: `/api/admin/users/${userId}/ban`,
    method: 'post',
    params: {
      minutes,
      reason
    }
  })
}

/**
 * 解除用户封禁（仅管理员）
 * @param userId 用户ID
 */
export function unbanUser(userId: number) {
  return request({
    url: `/api/admin/users/${userId}/unban`,
    method: 'post'
  })
}

/**
 * 获取用户封禁状态（仅管理员）
 * @param userId 用户ID
 */
export function getUserBanStatus(userId: number) {
  return request({
    url: `/api/admin/users/${userId}/ban-status`,
    method: 'get'
  })
} 