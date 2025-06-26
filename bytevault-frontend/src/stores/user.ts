import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, logout, getUserInfo } from '@/api/auth'
import router from '@/router'

export interface UserInfo {
  id: number
  username: string
  status: number
  roles?: string[]
}

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)
  const loading = ref<boolean>(false)
  
  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const hasRole = computed(() => (role: string) => {
    return userInfo.value?.roles?.includes(role) || false
  })
  
  // 动作
  async function loginAction(username: string, password: string) {
    try {
      loading.value = true
      const res = await login(username, password)
      token.value = res.data.token
      localStorage.setItem('token', token.value)
      await fetchUserInfo()
      
      // 登录成功后跳转
      const redirectPath = router.currentRoute.value.query.redirect as string || '/'
      router.push(redirectPath)
      
      return true
    } catch (error) {
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
      userInfo.value = res.data
    } catch (error) {
      console.error('获取用户信息失败:', error)
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
  
  // 初始化函数 - 如果有token则自动获取用户信息
  function init() {
    if (token.value) {
      fetchUserInfo()
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
    init
  }
}) 