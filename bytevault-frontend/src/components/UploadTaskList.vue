<template>
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
      <div v-for="(task, index) in tasks" :key="index" class="task-item">
        <div class="task-info">
          <div class="file-icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="task-details">
            <div class="task-name">{{ task.fileName }}</div>
            <div class="task-meta">
              <span class="file-size">{{ formatFileSize(task.fileSize) }}</span>
              <span class="task-status" :class="getStatusClass(task)">{{ getStatusText(task) }}</span>
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
              @click="pauseTask(index)"
              type="warning"
              circle
            >
              <el-icon><VideoPause /></el-icon>
            </el-button>
            <el-button 
              v-if="task.status === 'paused'" 
              size="small" 
              @click="resumeTask(index)"
              type="success"
              circle
            >
              <el-icon><VideoPlay /></el-icon>
            </el-button>
            <el-button 
              v-if="task.status !== 'completed'" 
              size="small" 
              @click="cancelTask(index)"
              type="danger"
              circle
            >
              <el-icon><Close /></el-icon>
            </el-button>
            <el-button 
              v-if="task.status === 'completed'" 
              size="small" 
              @click="removeTask(index)"
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
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, VideoPause, VideoPlay, Close, Delete } from '@element-plus/icons-vue'
import { fileApi } from '@/api'

// 定义任务状态类型
type TaskStatus = 'uploading' | 'paused' | 'completed' | 'error'

// 定义任务类型
interface UploadTask {
  fileName: string
  fileSize: number
  uploadedSize: number
  progress: number
  status: TaskStatus
  uploadId: string
  file: File
  totalChunks: number
  uploadedChunks: number[]
  parentId: number
  isPublic: boolean
  abortController: AbortController | null
}

// 任务列表
const tasks = ref<UploadTask[]>([])

// 计算属性：是否有已完成的任务
const hasCompletedTasks = computed(() => {
  return tasks.value.some(task => task.status === 'completed')
})

// 添加新任务
const addTask = (file: File, parentId: number = 0, isPublic: boolean = false) => {
  console.log('添加上传任务', file.name, file.size)
  
  const newTask: UploadTask = {
    fileName: file.name,
    fileSize: file.size,
    uploadedSize: 0,
    progress: 0,
    status: 'uploading',
    uploadId: '',
    file,
    totalChunks: 0,
    uploadedChunks: [],
    parentId,
    isPublic,
    abortController: new AbortController()
  }
  
  tasks.value.push(newTask)
  const taskIndex = tasks.value.length - 1
  console.log('当前任务列表', tasks.value)
  
  // 开始上传任务
  startUploadTask(taskIndex)
  
  return taskIndex
}

// 开始上传任务
const startUploadTask = async (taskIndex: number) => {
  const task = tasks.value[taskIndex]
  if (!task || task.status !== 'uploading') return
  
  try {
    const CHUNK_SIZE = 6 * 1024 * 1024 // 6MB 分块大小
    
    // 初始化上传
    const initResponse: any = await fileApi.initChunkUpload(
      task.file.name,
      task.file.size,
      task.file.type,
      task.parentId,
      task.isPublic
    )
    
    if (!initResponse || !initResponse.uploadId) {
      throw new Error('初始化上传失败')
    }
    
    task.uploadId = initResponse.uploadId
    
    // 获取已上传的分块列表
    const chunksResponse: any = await fileApi.getUploadedChunks(task.uploadId)
    task.uploadedChunks = chunksResponse?.uploadedChunks || []
    
    // 计算总分块数
    task.totalChunks = Math.ceil(task.file.size / CHUNK_SIZE)
    
    // 更新已上传大小
    task.uploadedSize = task.uploadedChunks.length * CHUNK_SIZE
    if (task.uploadedChunks.length > 0 && task.uploadedSize > task.file.size) {
      task.uploadedSize = task.file.size
    }
    
    // 更新进度
    task.progress = (task.uploadedSize / task.file.size) * 100
    
    // 上传分块
    for (let i = 0; i < task.totalChunks; i++) {
      // 如果任务状态不是上传中，则退出循环
      if (task.status !== 'uploading') {
        return
      }
      
      if (task.uploadedChunks.includes(i)) {
        continue
      }
      
      // 计算分块范围
      const start = i * CHUNK_SIZE
      const end = Math.min(task.file.size, start + CHUNK_SIZE)
      const chunk = task.file.slice(start, end)
      
      // 上传分块
      await fileApi.uploadChunk(task.uploadId, i, chunk)
      
      // 更新已上传分块和进度
      task.uploadedChunks.push(i)
      task.uploadedSize += chunk.size
      task.progress = (task.uploadedSize / task.file.size) * 100
    }
    
    // 所有分块上传完成，合并文件
    if (task.uploadedChunks.length === task.totalChunks) {
      await fileApi.completeChunkUpload(task.uploadId, task.totalChunks)
      task.status = 'completed'
      task.progress = 100
      ElMessage.success(`文件 ${task.fileName} 上传成功`)
    }
  } catch (error) {
    // 检查任务是否被暂停
    if (task && (task.status as TaskStatus) === 'paused') {
      console.log('上传已暂停')
    } else {
      console.error('上传失败:', error)
      if (task) {
        task.status = 'error'
        ElMessage.error(`文件 ${task.fileName} 上传失败`)
      }
    }
  }
}

// 暂停任务
const pauseTask = (taskIndex: number) => {
  const task = tasks.value[taskIndex]
  if (!task) return
  
  task.status = 'paused'
  task.abortController?.abort()
  task.abortController = null
  ElMessage.info(`已暂停上传 ${task.fileName}`)
}

// 继续任务
const resumeTask = (taskIndex: number) => {
  const task = tasks.value[taskIndex]
  if (!task) return
  
  task.status = 'uploading'
  task.abortController = new AbortController()
  startUploadTask(taskIndex)
  ElMessage.success(`继续上传 ${task.fileName}`)
}

// 取消任务
const cancelTask = async (taskIndex: number) => {
  try {
    await ElMessageBox.confirm('确定要取消此上传任务吗？', '取消上传', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const task = tasks.value[taskIndex]
    if (!task) return
    
    task.abortController?.abort()
    tasks.value.splice(taskIndex, 1)
    ElMessage.info('已取消上传任务')
  } catch (error) {
    // 用户取消了确认对话框
  }
}

// 移除任务
const removeTask = (taskIndex: number) => {
  tasks.value.splice(taskIndex, 1)
}

// 清除已完成任务
const clearCompletedTasks = () => {
  tasks.value = tasks.value.filter(task => task.status !== 'completed')
}

// 获取任务状态文本
const getStatusText = (task: UploadTask) => {
  switch (task.status) {
    case 'uploading': return '上传中'
    case 'paused': return '已暂停'
    case 'completed': return '已完成'
    case 'error': return '上传失败'
    default: return '未知状态'
  }
}

// 获取任务状态样式类
const getStatusClass = (task: UploadTask) => {
  switch (task.status) {
    case 'uploading': return 'status-uploading'
    case 'paused': return 'status-paused'
    case 'completed': return 'status-completed'
    case 'error': return 'status-error'
    default: return ''
  }
}

// 格式化文件大小
const formatFileSize = (size: number) => {
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let index = 0
  let convertedSize = size
  
  while (convertedSize >= 1024 && index < units.length - 1) {
    convertedSize /= 1024
    index++
  }
  
  let decimalPlaces = 0
  if (index === 1) decimalPlaces = 1
  else if (index >= 2) decimalPlaces = 2
  
  return convertedSize.toFixed(decimalPlaces).replace(/\.0+$/, '') + ' ' + units[index]
}

// 暴露方法给父组件
defineExpose({
  addTask,
  tasks
})
</script>

<style scoped>
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