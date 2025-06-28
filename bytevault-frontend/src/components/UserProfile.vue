<template>
  <div class="anime-profile-container">
    <div class="anime-profile">
      <div class="anime-profile-header">
        <div class="header-bg"></div>
        <div class="avatar-frame">
          <el-avatar
            :size="120"
            :src="avatarUrl || defaultAvatar"
            @error="handleAvatarError"
          >
            {{ userStore.userInfo?.username.charAt(0).toUpperCase() }}
          </el-avatar>
        </div>
      </div>
      
      <div class="anime-profile-content">
        <h2 class="username">{{ userStore.userInfo?.username }}</h2>
        <div class="user-roles" v-if="userStore.userInfo?.roles && userStore.userInfo.roles.length > 0">
          <span class="role-tag" v-for="(role, index) in processedRoles" :key="index">
            {{ role }}
          </span>
        </div>
        
        <div class="anime-card">
          <div class="card-title">
            <i class="card-icon el-icon-picture-outline"></i>
            个人头像
          </div>
          <div class="card-content">
            <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :http-request="handleAvatarUpload"
              accept="image/*"
            >
              <el-button size="small" type="primary" class="anime-button">
                {{ userStore.userInfo?.avatarUrl ? '更换头像' : '上传头像' }}
              </el-button>
            </el-upload>
            
            <el-button 
              v-if="userStore.userInfo?.avatarUrl" 
              size="small" 
              type="danger" 
              class="anime-button delete-button"
              @click="handleDeleteAvatar"
            >
              删除头像
            </el-button>
          </div>
        </div>
        
        <div class="anime-card">
          <div class="card-title">
            <i class="card-icon el-icon-user"></i>
            我的信息
          </div>
          <div class="card-content">
            <div class="info-item">
              <span class="info-label">用户名</span>
              <span class="info-value">{{ userStore.userInfo?.username }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">状态</span>
              <span class="info-value status-active">
                <span class="status-dot"></span>
                活跃
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">等级</span>
              <div class="level-bar">
                <div class="level-progress"></div>
                <span class="level-text">Lv.1</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { uploadAvatar, deleteAvatar } from '@/api/user'
import { ElMessage } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'

const userStore = useUserStore()
const defaultAvatar = ref('/src/assets/default-avatar.png')
const avatarUrl = computed(() => {
  const url = userStore.userInfo?.avatarUrl
  if (!url) return defaultAvatar.value
  
  // 如果是相对URL，需要添加BASE_URL
  if (url.startsWith('/api/')) {
    return url
  }
  return url
})

// 处理后的角色列表
const processedRoles = computed(() => {
  const roles = userStore.userInfo?.roles || []
  if (!roles || roles.length === 0) return ['普通用户']
  
  // 处理后端返回的角色对象数组，提取角色名称
  return roles.map((role: any) => {
    if (typeof role === 'string') return role
    if (role && typeof role === 'object' && role.name) return role.name
    return '未知角色'
  })
})

// 格式化角色显示
const formatRoles = (roles: any[]) => {
  if (!roles || roles.length === 0) return '无角色'
  
  // 处理后端返回的角色对象数组，提取角色名称
  return roles.map((role: any) => {
    if (typeof role === 'string') return role
    if (role && typeof role === 'object' && role.name) return role.name
    return '未知角色'
  }).join(', ')
}

// 头像加载错误处理
const handleAvatarError = () => {
  console.error('头像加载失败')
}

// 上传前验证
const beforeAvatarUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('头像必须是图片格式!')
    return false
  }
  
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!')
    return false
  }
  
  return true
}

// 自定义上传处理
const handleAvatarUpload = async (options: UploadRequestOptions) => {
  try {
    const file = options.file as File
    const response: any = await uploadAvatar(file)
    
    // 更新用户信息中的头像URL
    if (userStore.userInfo) {
      userStore.userInfo.avatarUrl = response.avatarUrl
    }
    
    ElMessage.success('头像上传成功')
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败')
  }
}

// 删除头像
const handleDeleteAvatar = async () => {
  try {
    await deleteAvatar()
    
    // 清除用户信息中的头像URL
    if (userStore.userInfo) {
      userStore.userInfo.avatarUrl = undefined
    }
    
    ElMessage.success('头像已删除')
  } catch (error) {
    console.error('头像删除失败:', error)
    ElMessage.error('头像删除失败')
  }
}
</script>

<style scoped>
.anime-profile-container {
  display: flex;
  justify-content: center;
  padding: 20px;
  min-height: 80vh;
  background-color: #f8f9fc;
  background-image: url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI1IiBoZWlnaHQ9IjUiPgo8cmVjdCB3aWR0aD0iNSIgaGVpZ2h0PSI1IiBmaWxsPSIjZmZmIj48L3JlY3Q+CjxyZWN0IHdpZHRoPSIxIiBoZWlnaHQ9IjEiIGZpbGw9IiNmMGYyZjUiPjwvcmVjdD4KPC9zdmc+');
}

.anime-profile {
  width: 100%;
  max-width: 800px;
  background-color: white;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(149, 157, 165, 0.1);
  overflow: hidden;
  position: relative;
}

.anime-profile-header {
  position: relative;
  height: 200px;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  background: linear-gradient(45deg, #7579ff, #b224ef);
  background-size: 400% 400%;
  animation: gradient 15s ease infinite;
}

@keyframes gradient {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.avatar-frame {
  position: absolute;
  bottom: -60px;
  left: 50%;
  transform: translateX(-50%);
  background: white;
  border-radius: 50%;
  padding: 6px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  z-index: 2;
}

.avatar-frame :deep(.el-avatar) {
  border: 4px solid white;
  box-shadow: 0 0 0 2px #7579ff;
}

.anime-profile-content {
  padding: 70px 30px 30px;
}

.username {
  text-align: center;
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 8px;
  color: #333;
}

.user-roles {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
}

.role-tag {
  background: linear-gradient(to right, #7579ff, #b224ef);
  color: white;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(178, 36, 239, 0.2);
}

.anime-card {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  padding: 16px;
  margin-bottom: 24px;
  border: 1px solid #f0f0f0;
  transition: transform 0.3s, box-shadow 0.3s;
}

.anime-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  border-bottom: 2px solid #f5f5f5;
  padding-bottom: 8px;
}

.card-icon {
  margin-right: 8px;
  color: #7579ff;
}

.card-content {
  display: flex;
  gap: 12px;
  justify-content: center;
  padding: 8px;
}

.anime-button {
  background: linear-gradient(to right, #7579ff, #b224ef);
  border: none;
  color: white;
  transition: all 0.3s;
}

.anime-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(178, 36, 239, 0.3);
}

.delete-button {
  background: linear-gradient(to right, #ff758c, #ff7eb3);
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  color: #888;
  font-size: 14px;
}

.info-value {
  font-weight: 500;
  color: #333;
}

.status-active {
  display: flex;
  align-items: center;
  color: #4cd964;
  font-weight: 600;
}

.status-dot {
  width: 8px;
  height: 8px;
  background-color: #4cd964;
  border-radius: 50%;
  margin-right: 6px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(76, 217, 100, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(76, 217, 100, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(76, 217, 100, 0);
  }
}

.level-bar {
  position: relative;
  width: 120px;
  height: 20px;
  background-color: #f0f0f0;
  border-radius: 10px;
  overflow: hidden;
}

.level-progress {
  position: absolute;
  top: 0;
  left: 0;
  width: 65%;
  height: 100%;
  background: linear-gradient(to right, #7579ff, #b224ef);
  border-radius: 10px;
}

.level-text {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}
</style> 