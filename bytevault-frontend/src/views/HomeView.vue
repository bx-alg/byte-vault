<template>
  <div class="home-container">
    <el-container>
      <el-header>
        <div class="header-container">
          <div class="logo">
            <h2>ByteVault</h2>
          </div>
          <div class="user-info">
            <span>欢迎, {{ userStore.userInfo?.username }}</span>
            <el-dropdown @command="handleCommand">
              <span class="dropdown-link">
                <el-avatar 
                  :size="32" 
                  :src="userStore.userInfo?.avatarUrl || ''"
                  @error="avatarLoadError = true"
                >
                  <el-icon><UserFilled /></el-icon>
                </el-avatar>
                <el-icon class="el-icon--right"><arrow-down /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                  <el-dropdown-item command="settings">设置</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      
      <el-container>
        <el-aside width="200px">
          <el-menu
            :default-active="activeMenu"
            class="el-menu-vertical"
            @select="handleMenuSelect"
          >
            <el-menu-item index="my-files">
              <el-icon><Document /></el-icon>
              <span>我的文件</span>
            </el-menu-item>
            <el-menu-item index="public-files">
              <el-icon><Share /></el-icon>
              <span>公共文件</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        
        <el-main>
          <!-- 用户个人资料 -->
          <el-dialog
            v-model="showProfileDialog"
            title="个人资料"
            width="400px"
          >
            <user-profile />
          </el-dialog>
          
          <!-- 文件浏览器组件 -->
          <file-explorer 
            :type="activeMenu" 
            :title="menuTitle"
            v-model:loading="loading"
            ref="fileExplorer"
          />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserProfile from '@/components/UserProfile.vue'
import FileExplorer from '@/components/FileExplorer.vue'
import { Document, Share, ArrowDown, UserFilled } from '@element-plus/icons-vue'

const userStore = useUserStore()
const avatarLoadError = ref(false)
const showProfileDialog = ref(false)
const loading = ref(false)
const activeMenu = ref('my-files')
const fileExplorer = ref(null)

// 根据菜单项计算标题
const menuTitle = computed(() => {
  switch (activeMenu.value) {
    case 'my-files':
      return '我的文件'
    case 'public-files':
      return '公共文件'
    default:
      return '文件'
  }
})

// 下拉菜单命令处理
const handleCommand = (command: string) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logoutAction()
      ElMessage.success('已退出登录')
    }).catch(() => {})
  } else if (command === 'profile') {
    showProfileDialog.value = true
  } else if (command === 'settings') {
    ElMessage.info('功能开发中...')
  }
}

// 处理菜单选择
const handleMenuSelect = (key: string) => {
  activeMenu.value = key
}

// 组件挂载时获取用户信息
onMounted(() => {
  if (userStore.token && !userStore.userInfo) {
    userStore.fetchUserInfo()
  }
})
</script>

<style scoped>
.home-container {
  height: 100vh;
  width: 100%;
}

.el-header {
  background-color: #409EFF;
  color: white;
  line-height: 60px;
  padding: 0 20px;
}

.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo h2 {
  margin: 0;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-info span {
  margin-right: 15px;
}

.dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
}

.el-aside {
  background-color: #f8f9fa;
  color: #333;
}

.el-menu-vertical {
  height: 100%;
  border-right: none;
}

.el-main {
  background-color: #f5f7fa;
  padding: 20px;
}
</style> 