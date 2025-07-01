<template>
  <div class="app-container" :style="backgroundStyle">
    <el-container v-if="userStore.isLoggedIn">
      <el-header>
        <div class="header-content">
          <div class="logo">ByteVault</div>
          <el-menu
            :default-active="activeMenuItem"
            mode="horizontal"
            router
            background-color="#ffffff"
            text-color="#303133"
            active-text-color="#409EFF"
          >
            <el-menu-item index="/">首页</el-menu-item>
            <el-menu-item index="/admin" v-if="userStore.hasRole('admin')">用户管理</el-menu-item>
            <el-menu-item index="/background">背景设置</el-menu-item>
          </el-menu>
          <div class="user-actions">
            <el-dropdown @command="handleUserAction">
              <span class="user-dropdown">
                <el-avatar :size="32" :src="userStore.userInfo?.avatarUrl">
                  {{ userStore.userInfo?.username.charAt(0).toUpperCase() }}
                </el-avatar>
                <span class="username">{{ userStore.userInfo?.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-button @click="show = true">个人资料</el-button>
                    <el-dialog v-model="show" title="个人资料">
                      <UserProfile />
                    </el-dialog>
                  <el-dropdown-item command="background">背景设置</el-dropdown-item>
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      
      <el-main>
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
      
      <el-footer>
        <div class="footer-content">
          <p>&copy; {{ new Date().getFullYear() }} ByteVault. 保留所有权利。</p>
        </div>
      </el-footer>
    </el-container>
    
    <div v-else>
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import UserProfile from '@/components/UserProfile.vue'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const show = ref(false)

// 计算当前活动菜单项
const activeMenuItem = computed(() => route.path)

// 计算背景样式
const backgroundStyle = computed(() => {
  if (userStore.userInfo?.backgroundImageUrl) {
  
    return {
      backgroundImage: `url(${userStore.userInfo.backgroundImageUrl})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundAttachment: 'fixed',
      backgroundColor: 'rgba(255, 255, 255, 0.9)'
    }

  }
  return {}
})

// 处理用户下拉菜单操作
const handleUserAction = async (command: string) => {
  switch (command) {
    case 'logout':
      try {
        await ElMessageBox.confirm(
          '确定要退出登录吗？',
          '提示',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        await userStore.logoutAction()
      } catch (error) {
        // 用户取消操作
      }
      break
    case 'background':
      router.push('/background')
      break
  }
}

// 监听用户信息变化，获取背景图片
watch(() => userStore.userInfo, async (newUserInfo) => {
  if (newUserInfo) {
    await userStore.fetchBackgroundImages()
  }
}, { immediate: true })

// 初始化
onMounted(async () => {
  // 初始化用户存储
  userStore.init()
  
  // 如果用户已登录，获取背景图片
  if (userStore.isLoggedIn) {
    await userStore.fetchBackgroundImages()
  }
})
</script>

<style>
/* 全局样式 */
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}

/* 修复Element Plus的el-tag组件中的小点问题 - 彻底版 */
.el-tag .el-tag__content::after,
.el-tag::after,
.el-tag *::after,
.el-tag__content::after {
  display: none !important;
}

/* 强制所有el-tag相关元素不显示伪元素 */
[class*="el-tag"] *::after {
  display: none !important;
}

#app {
  height: 100%;
}

.app-container {
  height: 100vh;
  position: relative;
  display: flex;
  flex-direction: column;
}

/* 背景图片叠加层 */
.app-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.65); /* 调整透明度 */
  z-index: 0;
  pointer-events: none;
}

/* 头部样式 */
.el-header {
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 100;
  background-color: rgba(255, 255, 255, 0.95);
  height: auto !important;
  min-height: 60px;
}

.header-content {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 20px;
  flex-wrap: wrap;
}

.logo {
  font-size: 1.5rem;
  font-weight: bold;
  color: #409EFF;
  margin-right: 20px;
  white-space: nowrap;
}

.user-actions {
  margin-left: auto;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username {
  margin-left: 8px;
  white-space: nowrap;
}

/* 主要内容区样式 */
.el-main {
  padding: 15px;
  position: relative;
  z-index: 1;
  background-color: rgba(255, 255, 255, 0.55); /* 调整透明度 */
  flex: 1;
  overflow-y: auto;
}

/* 页脚样式 */
.el-footer {
  background-color: rgba(245, 247, 250, 0.9);
  padding: 0;
  height: 50px !important;
  position: relative;
  z-index: 100;
}

.footer-content {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #909399;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .header-content {
    padding: 10px;
    justify-content: space-between;
  }

  .el-menu.el-menu--horizontal {
    padding: 0;
    border: none;
  }

  .el-menu--horizontal > .el-menu-item {
    height: 50px;
    line-height: 50px;
    padding: 0 10px;
  }
  
  .el-main {
    padding: 10px;
  }
}
</style> 
