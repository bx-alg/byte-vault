import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, logout, getUserInfo, register } from '@/api/auth'
import router from '@/router'
import { uploadAvatar } from '@/api/user'

export interface UserInfo {
  id: number
  username: string
  status: number
  avatarUrl?: string
  roles?: any[]
}

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)
  const loading = ref<boolean>(false)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const hasRole = computed(() => (role: string) => {
    if (!userInfo.value?.roles || userInfo.value.roles.length === 0) {
      return false
    }
    
    return userInfo.value.roles.some(r => {
      // 处理字符串角色
      if (typeof r === 'string') {
        return r === role
      }
      
      // 处理对象角色 (role.name)
      if (r && typeof r === 'object' && r.name) {
        return r.name === role
      }
      
      return false
    })
  })

  // 动作
  async function loginAction(username: string, password: string) {
    try {
      loading.value = true
      const res = await login(username, password)
      token.value = res.token
      localStorage.setItem('token', token.value)
      userInfo.value = res.user

      // 登录成功后跳转
      const redirectPath = router.currentRoute.value.query.redirect as string || '/'
      router.push(redirectPath)

      return true
    } catch (error: any) {
      console.error('登录失败:', error)
      return false
    } finally {
      loading.value = false
    }
  }

  async function fetchUserInfo() {
    if (!token.value) return

    try {
      loading.value = true
      const res = await getUserInfo()
      userInfo.value = res
    } catch (error) {
      console.error('获取用户信息失败:', error)
      // 如果获取用户信息失败，可能是令牌已过期，清除本地状态
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
    } finally {
      loading.value = false
    }
  }

  async function logoutAction() {
    try {
      if (token.value) {
        await logout()
      }
    } catch (error) {
      console.error('登出失败:', error)
    } finally {
      // 无论是否成功调用登出API，都清除本地状态
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
      router.push('/login')
    }
  }

  // 检查用户会话状态
  async function checkSession() {
    if (token.value && !userInfo.value) {
      await fetchUserInfo()
    }
    return !!userInfo.value
  }

  // 初始化函数 - 如果有token则自动获取用户信息
  function init() {
    if (token.value) {
      fetchUserInfo()
    }
  }

  // 注册操作
  const registerAction = async (username: string, password: string, confirmPassword: string) => {
    try {
      loading.value = true
      const response = await register(username, password, confirmPassword)
      return response && response.username === username
    } catch (error) {
      console.error('注册失败:', error)
      return false
    } finally {
      loading.value = false
    }
  }

  // 上传头像
  const uploadAvatarAction = async (file: File) => {
    try {
      loading.value = true
      const response = await uploadAvatar(file)
      
      // 假设后端返回的数据结构中包含avatarUrl字段
      if (response && userInfo.value) {
        // 如果后端返回了新的头像URL，则更新用户信息
        if (response.data && response.data.avatarUrl) {
          userInfo.value.avatarUrl = response.data.avatarUrl
        }
        return true
      }
      return false
    } catch (error) {
      console.error('上传头像失败:', error)
      return false
    } finally {
      loading.value = false
    }
  }

  return {
    token,
    userInfo,
    loading,
    isLoggedIn,
    hasRole,
    loginAction,
    logoutAction,
    fetchUserInfo,
    checkSession,
    init,
    registerAction,
    uploadAvatar: uploadAvatarAction
  }
}) 