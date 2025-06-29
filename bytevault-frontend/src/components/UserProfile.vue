<template>
  <div class="user-profile">
    <div class="profile-header">
      <div class="profile-avatar-container">
        <div class="avatar-wrapper">
          <el-avatar :size="100" :src="avatarUrl || defaultAvatar" class="profile-avatar" />
          <div class="avatar-overlay" @click="triggerFileInput">
            <el-icon><Edit /></el-icon>
          </div>
        </div>
        <input
          type="file"
          ref="fileInput"
          style="display: none"
          accept="image/*"
          @change="handleAvatarChange"
        />
      </div>
      
      <div class="profile-info">
        <h2 class="username">{{ userStore.userInfo?.username || '用户名' }}</h2>
        <div class="user-roles">
          <el-tag v-for="role in userRoles" :key="role" class="role-tag">
            {{ role }}
          </el-tag>
        </div>
      </div>
    </div>
    
    <el-divider>
      <div class="divider-content">
        <el-icon><Star /></el-icon>
        <span>个人信息</span>
      </div>
    </el-divider>
    
    <div class="profile-form">
      <el-form :model="userForm" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="userForm.username" disabled />
        </el-form-item>
        
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        
        <el-form-item label="个人简介">
          <el-input
            v-model="userForm.bio"
            type="textarea"
            placeholder="介绍一下自己吧"
            :rows="3"
          />
        </el-form-item>
        
        <div class="form-actions">
          <el-button type="primary" @click="saveProfile" class="save-btn wiggle">
            保存修改
          </el-button>
        </div>
      </el-form>
    </div>
    
    <el-divider>
      <div class="divider-content">
        <el-icon><Lock /></el-icon>
        <span>修改密码</span>
      </div>
    </el-divider>
    
    <div class="password-form">
      <el-form :model="passwordForm" label-position="top">
        <el-form-item label="当前密码">
          <el-input
            v-model="passwordForm.currentPassword"
            type="password"
            placeholder="请输入当前密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="新密码">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="确认密码">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
        
        <div class="form-actions">
          <el-button type="primary" @click="changePassword" class="save-btn wiggle">
            修改密码
          </el-button>
        </div>
      </el-form>
    </div>
    
    <div class="profile-decoration">
      <img src="@/assets/_.jpeg" class="decoration-image floating" alt="装饰" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Edit, Star, Lock } from '@element-plus/icons-vue'

const userStore = useUserStore()
const fileInput = ref<HTMLInputElement | null>(null)
const defaultAvatar = 'https://i.imgur.com/Jvh1OQm.jpg'

// 用户头像
const avatarUrl = computed(() => {
  return userStore.userInfo?.avatarUrl || defaultAvatar
})

// 用户角色
const userRoles = computed(() => {
  const roles = userStore.userInfo?.roles || []
  return roles.length > 0 ? roles.map(role => role.name) : ['普通用户']
})

// 用户表单
const userForm = ref({
  username: userStore.userInfo?.username || '',
  email: '',
  bio: ''
})

// 密码表单
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 触发文件选择
const triggerFileInput = () => {
  fileInput.value?.click()
}

// 处理头像上传
const handleAvatarChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  if (!target.files || target.files.length === 0) return
  
  const file = target.files[0]
  
  try {
    const result = await userStore.uploadAvatar(file)
    if (result) {
      ElMessage.success('头像上传成功')
    } else {
      ElMessage.error('头像上传失败')
    }
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败')
  }
}

// 保存个人资料
const saveProfile = async () => {
  try {
    // 模拟保存成功
    ElMessage.success('个人资料保存成功')
  } catch (error) {
    ElMessage.error('保存失败，请稍后重试')
  }
}

// 修改密码
const changePassword = async () => {
  if (!passwordForm.value.currentPassword) {
    ElMessage.warning('请输入当前密码')
    return
  }
  
  if (!passwordForm.value.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  
  try {
    // 模拟修改成功
    ElMessage.success('密码修改成功')
    passwordForm.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  } catch (error) {
    ElMessage.error('密码修改失败，请稍后重试')
  }
}
</script>

<style scoped>
.user-profile {
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.profile-header {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
}

.profile-avatar-container {
  position: relative;
  margin-right: 30px;
}

.avatar-wrapper {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid var(--primary-color);
  transition: all 0.3s ease;
}

.avatar-wrapper:hover {
  transform: scale(1.05);
  box-shadow: 0 0 15px rgba(255, 105, 180, 0.5);
}

.profile-avatar {
  width: 100%;
  height: 100%;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s ease;
  cursor: pointer;
  color: white;
  font-size: 1.5rem;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.profile-info {
  flex: 1;
}

.username {
  margin: 0 0 10px 0;
  color: var(--primary-color);
  font-size: 1.8rem;
  font-weight: 700;
}

.user-roles {
  display: flex;
  gap: 8px;
}

.role-tag {
  border-radius: 20px;
  padding: 0 12px;
}

.divider-content {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--secondary-color);
  font-weight: 600;
}

.profile-form,
.password-form {
  max-width: 500px;
  margin: 0 auto 30px;
  background-color: rgba(255, 255, 255, 0.7);
  padding: 20px;
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
  transition: transform 0.3s ease;
}

.profile-form:hover,
.password-form:hover {
  transform: translateY(-5px);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.save-btn {
  min-width: 120px;
}

.profile-decoration {
  position: absolute;
  bottom: -20px;
  right: -20px;
  z-index: -1;
  opacity: 0.7;
}

.decoration-image {
  width: 150px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: var(--secondary-color);
}

:deep(.el-divider__text) {
  background-color: transparent;
}
</style> 