<template>
  <div class="user-profile">
    <div class="avatar-container">
      <el-avatar
        :size="100"
        :src="avatarUrl || defaultAvatar"
        @error="handleAvatarError"
      >
        {{ userStore.userInfo?.username.charAt(0).toUpperCase() }}
      </el-avatar>
      
      <div class="avatar-actions">
        <el-upload
          class="avatar-uploader"
          :show-file-list="false"
          :before-upload="beforeAvatarUpload"
          :http-request="handleAvatarUpload"
          accept="image/*"
        >
          <el-button size="small" type="primary">
            {{ userStore.userInfo?.avatarUrl ? '更换头像' : '上传头像' }}
          </el-button>
        </el-upload>
        
        <el-button 
          v-if="userStore.userInfo?.avatarUrl" 
          size="small" 
          type="danger" 
          @click="handleDeleteAvatar"
        >
          删除头像
        </el-button>
      </div>
    </div>
    
    <div class="user-info">
      <h3>{{ userStore.userInfo?.username }}</h3>
      <p v-if="userStore.userInfo?.roles">
        角色: {{ userStore.userInfo.roles.join(', ') }}
      </p>
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
.user-profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.avatar-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.user-info {
  text-align: center;
}

.user-info h3 {
  margin-bottom: 5px;
}

.user-info p {
  color: var(--el-text-color-secondary);
  margin: 0;
}
</style> 