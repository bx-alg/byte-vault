<template>
  <div class="background-settings">
    <el-card class="settings-card">
      <template #header>
        <div class="card-header">
          <span>背景设置</span>
          <el-upload
            class="upload-btn"
            :show-file-list="false"
            :http-request="customUpload"
            accept="image/*"
          >
            <el-button type="primary" :loading="loading">上传新背景</el-button>
          </el-upload>
        </div>
      </template>
      
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="3" animated />
      </div>
      
      <el-empty v-else-if="!backgroundImages.length" description="暂无背景图片">
        <el-upload
          class="upload-empty"
          :show-file-list="false"
          :http-request="customUpload"
          accept="image/*"
        >
          <el-button type="primary">上传第一张背景图片</el-button>
        </el-upload>
      </el-empty>
      
      <div v-else class="background-list">
        <el-row :gutter="16">
          <el-col v-for="image in backgroundImages" :key="image.id" :span="8" class="background-item-col">
            <div class="background-item" :class="{ active: isCurrentBackground(image) }">
              <div class="background-image">
                <img :src="image.imageUrl" alt="背景图片" />
              </div>
              <div class="background-actions">
                <el-button 
                  v-if="!isCurrentBackground(image)" 
                  type="success" 
                  size="small" 
                  @click="setAsBackground(image)"
                >
                  设为背景
                </el-button>
                <el-button 
                  v-else 
                  type="info" 
                  size="small" 
                  disabled
                >
                  当前背景
                </el-button>
                <el-button 
                  type="danger" 
                  size="small" 
                  @click="deleteBackground(image)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore, type BackgroundImage } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'

const userStore = useUserStore()
const loading = ref(false)

const backgroundImages = computed(() => userStore.backgroundImages)
const currentBackgroundUrl = computed(() => userStore.currentBackgroundUrl)

// 检查是否是当前背景
const isCurrentBackground = (image: BackgroundImage) => {
  return currentBackgroundUrl.value === image.imageUrl
}

// 自定义上传
const customUpload = async (options: UploadRequestOptions) => {
  const file = options.file as File
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return
  }
  
  // 验证文件大小 (最大5MB)
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过5MB')
    return
  }
  
  loading.value = true
  
  try {
    const success = await userStore.uploadBackgroundImage(file)
    if (success) {
      ElMessage.success('背景图片上传成功')
    } else {
      ElMessage.error('背景图片上传失败')
    }
  } catch (error) {
    console.error('上传背景图片失败:', error)
    ElMessage.error('上传背景图片失败')
  } finally {
    loading.value = false
  }
}

// 设置为背景
const setAsBackground = async (image: BackgroundImage) => {
  loading.value = true
  
  try {
    const success = await userStore.setCurrentBackgroundImage(image.id)
    if (success) {
      ElMessage.success('背景图片设置成功')
    } else {
      ElMessage.error('背景图片设置失败')
    }
  } catch (error) {
    console.error('设置背景图片失败:', error)
    ElMessage.error('设置背景图片失败')
  } finally {
    loading.value = false
  }
}

// 删除背景
const deleteBackground = async (image: BackgroundImage) => {
  try {
    const confirmed = await ElMessageBox.confirm(
      '确定要删除这张背景图片吗？',
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    if (confirmed) {
      loading.value = true
      const success = await userStore.deleteBackgroundImage(image.id)
      if (success) {
        ElMessage.success('背景图片删除成功')
      } else {
        ElMessage.error('背景图片删除失败')
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除背景图片失败:', error)
      ElMessage.error('删除背景图片失败')
    }
  } finally {
    loading.value = false
  }
}

// 组件挂载时获取背景图片列表
onMounted(async () => {
  loading.value = true
  try {
    await userStore.fetchBackgroundImages()
  } catch (error) {
    console.error('获取背景图片列表失败:', error)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.background-settings {
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
}

.settings-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-container {
  padding: 20px 0;
}

.background-list {
  margin-top: 20px;
}

.background-item-col {
  margin-bottom: 20px;
}

.background-item {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.background-item.active {
  box-shadow: 0 0 0 2px #67c23a;
}

.background-image {
  height: 150px;
  overflow: hidden;
}

.background-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.background-actions {
  display: flex;
  justify-content: space-around;
  padding: 10px;
  background-color: #f5f7fa;
}

.upload-empty {
  margin-top: 20px;
}
</style> 