import request from '@/utils/request'
import type { UserInfo } from '@/stores/user'

export interface User extends UserInfo {
  createTime?: string
  updateTime?: string
}

export interface Role {
  id: number
  name: string
  description?: string
  permissions?: any[]
}

/**
 * 获取所有用户
 */
export function getAllUsers(): Promise<User[]> {
  return request({
    url: '/api/admin/users',
    method: 'get'
  })
}

/**
 * 获取所有角色
 */
export function getAllRoles(): Promise<Role[]> {
  return request({
    url: '/api/admin/roles',
    method: 'get'
  })
}

/**
 * 更新用户信息
 * @param userId 用户ID
 * @param data 用户数据
 */
export function updateUser(userId: number, data: Partial<User>): Promise<User> {
  return request({
    url: `/api/users/${userId}`,
    method: 'put',
    data
  })
}

/**
 * 为用户分配角色
 * @param userId 用户ID
 * @param roleIds 角色ID列表
 */
export function assignRolesToUser(userId: number, roleIds: number[]): Promise<any> {
  return request({
    url: `/api/admin/users/${userId}/roles`,
    method: 'post',
    data: roleIds
  })
}

/**
 * 封禁用户
 * @param userId 用户ID
 * @param minutes 封禁时间(分钟)
 * @param reason 封禁原因
 */
export function banUser(userId: number, minutes: number = 60, reason: string = '违反用户协议'): Promise<any> {
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
 * 解除用户封禁
 * @param userId 用户ID
 */
export function unbanUser(userId: number): Promise<any> {
  return request({
    url: `/api/admin/users/${userId}/unban`,
    method: 'post'
  })
}

/**
 * 获取用户封禁状态
 * @param userId 用户ID
 */
export function getUserBanStatus(userId: number): Promise<any> {
  return request({
    url: `/api/admin/users/${userId}/ban-status`,
    method: 'get'
  })
}

/**
 * 修改用户状态
 * @param userId 用户ID
 * @param status 用户状态 1-正常 0-禁用
 */
export function updateUserStatus(userId: number, status: number): Promise<User> {
  return request({
    url: `/api/users/${userId}`,
    method: 'put',
    data: { status }
  })
}

/**
 * 批量修改用户状态
 * @param userIds 用户ID列表
 * @param status 用户状态 1-正常 0-禁用
 */
export function batchUpdateUserStatus(userIds: number[], status: number): Promise<any[]> {
  return Promise.all(userIds.map(userId => updateUserStatus(userId, status)))
} 