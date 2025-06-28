<template>
  <div class="app-container">
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
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'
import { ref } from 'vue'
import UserProfile from '@/components/UserProfile.vue'

const userStore = useUserStore()
const route = useRoute()
const show = ref(false)

// 计算当前活动菜单项
const activeMenuItem = computed(() => route.path)

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
  }
}

// 初始化
onMounted(() => {
  // 初始化用户存储
  userStore.init()
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

#app {
  height: 100%;
}

.app-container {
  height: 100vh;
}

/* 头部样式 */
.el-header {
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 100;
}

.header-content {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.logo {
  font-size: 1.5rem;
  font-weight: bold;
  color: #409EFF;
  margin-right: 20px;
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
}

/* 主要内容区样式 */
.el-main {
  padding: 20px;
  height: calc(100vh - 120px);
  overflow-y: auto;
}

/* 页脚样式 */
.el-footer {
  background-color: #f5f7fa;
  padding: 0;
  height: 60px;
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
</style> 