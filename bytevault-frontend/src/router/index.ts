import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import BackgroundSettings from '@/components/BackgroundSettings.vue'

// 环境变量类型声明
declare global {
  interface ImportMeta {
    env: {
      BASE_URL: string
    }
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { guest: true }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/components/UserProfile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/background',
    name: 'Background',
    component: BackgroundSettings,
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/AdminView.vue'),
    meta: { 
      requiresAuth: true,
      requiresAdmin: true 
    }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL || ''),
  routes,
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 检查路由是否需要身份验证
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const requiresAdmin = to.matched.some(record => record.meta.requiresAdmin)
  const isGuestOnly = to.matched.some(record => record.meta.guest)
  
  // 检查用户会话状态
  const isAuthenticated = await userStore.checkSession()
  
  // 根据路由需求和用户状态决定路由行为
  if (requiresAuth && !isAuthenticated) {
    // 需要登录的页面，但用户未登录
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  } else if (requiresAdmin && isAuthenticated) {
    // 确保用户信息已加载
    if (!userStore.userInfo) {
      await userStore.fetchUserInfo()
    }
    
    // 检查是否是管理员
    if (!userStore.hasRole('admin')) {
      ElMessage.error('没有权限访问此页面')
      next({ path: '/' })
    } else {
      next()
    }
  } else if (isGuestOnly && isAuthenticated) {
    // 仅供游客访问的页面，但用户已登录
    next({ path: '/' })
  } else {
    // 正常导航
    next()
  }
})

export default router 