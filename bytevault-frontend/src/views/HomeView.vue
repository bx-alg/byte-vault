<template>
  <div class="home-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="title-container">
        <img src="https://i.imgur.com/QKnfQiS.png" class="title-decoration floating" alt="装饰" />
        <h1 class="main-title">ByteVault <span class="subtitle">文件管理系统</span></h1>
      </div>
      
      <div class="user-greeting" v-if="userStore.userInfo">
        <span class="greeting-text">欢迎回来，{{ userStore.userInfo.username }} !</span>
        <div class="avatar-container">
          <el-avatar :src="userStore.userInfo.avatarUrl || 'https://i.imgur.com/Jvh1OQm.jpg'" />
        </div>
      </div>
    </div>
    
    <!-- 文件浏览器切换标签 -->
    <div class="tabs-container">
      <el-tabs v-model="activeTab" @tab-click="handleTabChange" class="custom-tabs">
        <el-tab-pane label="我的文件" name="my-files">
          <template #label>
            <div class="tab-label">
              <el-icon><Document /></el-icon>
              <span>我的文件</span>
            </div>
          </template>
        </el-tab-pane>
        
        <el-tab-pane label="公共文件" name="public-files">
          <template #label>
            <div class="tab-label">
              <el-icon><Share /></el-icon>
              <span>公共文件</span>
            </div>
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>
    
    <!-- 文件浏览器组件 -->
    <FileExplorer 
      :type="activeTab"
      :title="activeTab === 'my-files' ? '我的文件' : '公共文件'"
      v-model:loading="loading"
      ref="fileExplorer"
    />
    
    <!-- 页脚装饰 -->
    <div class="footer-decoration">
      <img src="https://i.imgur.com/8nLFCVn.png" class="footer-image" alt="页脚装饰" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { Document, Share } from '@element-plus/icons-vue'
import FileExplorer from '@/components/FileExplorer.vue'

// 用户信息
const userStore = useUserStore()

// 文件浏览器相关
const activeTab = ref('my-files')
const loading = ref(false)
const fileExplorer = ref<InstanceType<typeof FileExplorer> | null>(null)

// 处理标签切换
const handleTabChange = () => {
  // 重置文件浏览器状态
  if (fileExplorer.value) {
    fileExplorer.value.resetState()
    fileExplorer.value.loadFiles()
  }
}

// 组件挂载时检查用户登录状态
onMounted(() => {
  if (!userStore.token) {
    userStore.init()
  }
})
</script>

<style scoped>
.home-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  position: relative;
}

.title-container {
  display: flex;
  align-items: center;
  position: relative;
}

.title-decoration {
  width: 60px;
  position: absolute;
  left: -30px;
  top: -20px;
  z-index: 1;
}

.main-title {
  font-size: 2.5rem;
  font-weight: 800;
  color: var(--primary-color);
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
  margin: 0;
  position: relative;
  z-index: 2;
}

.subtitle {
  font-size: 1.2rem;
  color: var(--secondary-color);
  margin-left: 10px;
  font-weight: normal;
}

.user-greeting {
  display: flex;
  align-items: center;
  gap: 10px;
}

.greeting-text {
  font-size: 1.1rem;
  color: var(--text-color);
}

.tabs-container {
  margin-bottom: 20px;
}

.custom-tabs :deep(.el-tabs__item) {
  font-size: 1.1rem;
  padding: 0 20px;
}

.custom-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--primary-color);
  height: 3px;
  border-radius: 3px;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 5px;
}

.footer-decoration {
  display: flex;
  justify-content: flex-end;
  margin-top: 40px;
}

.footer-image {
  width: 150px;
  opacity: 0.8;
}
</style> 