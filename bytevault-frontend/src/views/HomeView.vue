<template>
  <div class="home-container">
    <el-container>
      <el-header>
        <div class="header-container">
          <div class="logo">
            <h2>ByteVault</h2>
          </div>
          <div class="user-info">
            <span>{{ userStore.userInfo?.username }}</span>
            <el-dropdown @command="handleCommand">
              <span class="dropdown-link">
                <el-avatar :size="32" icon="el-icon-user" />
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人信息</el-dropdown-item>
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
            default-active="1"
            class="el-menu-vertical"
            :router="true"
          >
            <el-menu-item index="1">
              <i class="el-icon-document"></i>
              <span>我的文件</span>
            </el-menu-item>
            <el-menu-item index="2">
              <i class="el-icon-share"></i>
              <span>共享文件</span>
            </el-menu-item>
            <el-menu-item index="3">
              <i class="el-icon-star-off"></i>
              <span>收藏夹</span>
            </el-menu-item>
            <el-menu-item index="4">
              <i class="el-icon-delete"></i>
              <span>回收站</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        
        <el-main>
          <el-card>
            <template #header>
              <div class="card-header">
                <span>欢迎使用ByteVault文件管理系统</span>
              </div>
            </template>
            <div>
              <p>您已成功登录系统。</p>
              <p>当前登录用户: {{ userStore.userInfo?.username }}</p>
              <p>用户ID: {{ userStore.userInfo?.id }}</p>
            </div>
          </el-card>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()

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
    ElMessage.info('功能开发中...')
  } else if (command === 'settings') {
    ElMessage.info('功能开发中...')
  }
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style> 