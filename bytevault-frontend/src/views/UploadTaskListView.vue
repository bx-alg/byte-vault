<template>
  <div class="upload-task-page">
    <div class="page-header">
      <h1 class="page-title">上传任务列表</h1>
      <div class="header-actions">
        <el-button type="primary" @click="navigateToFiles" class="wiggle">
          <el-icon><Back /></el-icon>
          返回文件管理
        </el-button>
      </div>
    </div>
    
    <div class="page-content">
      <!-- 直接在这里渲染任务列表，而不是使用组件 -->
      <div class="upload-task-list">
        <div class="task-list-header">
          <h2>上传任务列表</h2>
          <div class="header-actions">
            <el-button size="small" type="primary" @click="clearCompletedTasks" :disabled="!hasCompletedTasks">
              清除已完成
            </el-button>
          </div>
        </div>

        <el-empty v-if="tasks.length === 0" description="暂无上传任务" class="empty-tasks">
          <img src="@/assets/cute.jpeg" class="empty-image floating" alt="暂无任务" />
        </el-empty>

        <div v-else class="task-list">
          <div v-for="task in tasks" :key="task.id" class="task-item">
            <div class="task-info">
              <div class="file-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="task-details">
                <div class="task-name">{{ task.fileName }}</div>
                <div class="task-meta">
                  <span class="file-size">{{ formatFileSize(task.fileSize) }}</span>
                  <span class="task-status" :class="getStatusClass(task.status)">{{ getStatusText(task.status) }}</span>
                </div>
              </div>
            </div>

            <div class="task-progress">
              <div v-if="task.status === 'uploading' || task.status === 'paused'" class="progress-container">
                <div class="kawaii-progress-bar">
                  <div class="kawaii-character" :style="{ left: `${task.progress}%` }">
                    <div class="character-face">
                      <div class="eyes"></div>
                      <div class="mouth" :class="{ 'happy-mouth': task.progress > 80 }"></div>
                    </div>
                  </div>
                  <div class="progress-track">
                    <div class="progress-fill" :style="{ width: `${task.progress}%` }"></div>
                  </div>
                </div>
                <div class="progress-text">
                  {{ Math.floor(task.progress) }}% - {{ formatFileSize(task.uploadedSize) }} / {{ formatFileSize(task.fileSize) }}
                </div>
              </div>

              <div class="task-actions">
                <el-button 
                  v-if="task.status === 'uploading'" 
                  size="small" 
                  @click="pauseTask(task.id)"
                  type="warning"
                  circle
                >
                  <el-icon><VideoPause /></el-icon>
                </el-button>
                <el-button 
                  v-if="task.status === 'paused'" 
                  size="small" 
                  @click="resumeTask(task.id)"
                  type="success"
                  circle
                >
                  <el-icon><VideoPlay /></el-icon>
                </el-button>
                <el-button 
                  v-if="task.status !== 'completed'" 
                  size="small" 
                  @click="cancelTask(task.id)"
                  type="danger"
                  circle
                >
                  <el-icon><Close /></el-icon>
                </el-button>
                <el-button 
                  v-if="task.status === 'completed'" 
                  size="small" 
                  @click="removeTask(task.id)"
                  type="info"
                  circle
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUploadTaskStore } from '@/stores/uploadTask'
import { Back, Document, VideoPause, VideoPlay, Close, Delete } from '@element-plus/icons-vue'

const router = useRouter()
const uploadTaskStore = useUploadTaskStore()

// 使用store中的状态和方法
const { tasks, hasCompletedTasks, clearCompletedTasks, pauseTask, resumeTask, cancelTask, removeTask, getStatusText, getStatusClass, formatFileSize } = uploadTaskStore

// 返回文件管理页面
const navigateToFiles = () => {
  router.push('/')
}




</script>

<style scoped>
.upload-task-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 15px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 1.8rem;
  color: var(--primary-color);
  margin: 0;
}

.page-content {
  background-color: #fff;
  border-radius: 8px;
  min-height: 400px;
}

/* 上传任务列表样式 */
.upload-task-list {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 16px;
  margin-bottom: 20px;
}

.task-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.task-list-header h2 {
  font-size: 18px;
  margin: 0;
  color: var(--primary-color);
}

.empty-tasks {
  padding: 30px 0;
}

.empty-image {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
}

.floating {
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
  100% { transform: translateY(0px); }
}

.task-list {
  max-height: 400px;
  overflow-y: auto;
}

.task-item {
  display: flex;
  flex-direction: column;
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.task-item:last-child {
  border-bottom: none;
}

.task-info {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.file-icon {
  font-size: 24px;
  margin-right: 12px;
  color: #909399;
}

.task-details {
  flex: 1;
}

.task-name {
  font-weight: 500;
  margin-bottom: 4px;
  word-break: break-all;
}

.task-meta {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.file-size {
  margin-right: 12px;
}

.task-status {
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 12px;
}

.status-uploading {
  background-color: #e6f7ff;
  color: #1890ff;
}

.status-paused {
  background-color: #fff7e6;
  color: #fa8c16;
}

.status-completed {
  background-color: #f6ffed;
  color: #52c41a;
}

.status-error {
  background-color: #fff1f0;
  color: #f5222d;
}

.task-progress {
  margin-top: 8px;
}

.progress-container {
  margin-bottom: 8px;
}

.kawaii-progress-bar {
  position: relative;
  height: 20px;
  background-color: #f5f5f5;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 4px;
}

.kawaii-character {
  position: absolute;
  top: -10px;
  width: 20px;
  height: 20px;
  transform: translateX(-50%);
  z-index: 2;
  transition: left 0.3s ease;
}

.character-face {
  width: 100%;
  height: 100%;
  background-color: #ffcc00;
  border-radius: 50%;
  position: relative;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.eyes {
  position: absolute;
  width: 8px;
  height: 2px;
  background-color: #333;
  top: 7px;
  left: 6px;
}

.eyes:before {
  content: "";
  position: absolute;
  width: 2px;
  height: 2px;
  background-color: #333;
  left: -4px;
  border-radius: 50%;
}

.mouth {
  position: absolute;
  width: 6px;
  height: 2px;
  background-color: #333;
  bottom: 5px;
  left: 7px;
  border-radius: 2px;
}

.happy-mouth {
  height: 4px;
  border-radius: 50% 50% 0 0;
  transform: rotate(180deg);
}

.progress-track {
  height: 100%;
  width: 100%;
  background-color: #f5f5f5;
  border-radius: 10px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #67c23a, #409eff);
  transition: width 0.3s ease;
}

.progress-text {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #606266;
}

.task-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .page-title {
    font-size: 1.5rem;
  }
  
  .task-item {
    padding: 10px;
  }
  
  .file-icon {
    font-size: 20px;
  }
  
  .task-actions {
    flex-wrap: wrap;
  }
}
</style>