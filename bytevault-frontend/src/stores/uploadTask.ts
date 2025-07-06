import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { fileApi } from '@/api'

// 定义任务状态类型
export type TaskStatus = 'uploading' | 'paused' | 'completed' | 'error'

// 定义任务类型
export interface UploadTask {
  id: string
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
  createTime: number
  failedChunks: Set<number> // 失败的分块索引
  retryCount: Map<number, number> // 每个分块的重试次数
}

export const useUploadTaskStore = defineStore('uploadTask', () => {
  // 状态
  const tasks = ref<UploadTask[]>([])
  const isUploading = ref(false)

  // 计算属性
  const hasCompletedTasks = computed(() => {
    return tasks.value.some(task => task.status === 'completed')
  })

  const uploadingTasks = computed(() => {
    return tasks.value.filter(task => task.status === 'uploading')
  })

  const completedTasks = computed(() => {
    return tasks.value.filter(task => task.status === 'completed')
  })

  // 生成唯一ID
  const generateTaskId = () => {
    return `task_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }

  // 添加新任务
  const addTask = (file: File, parentId: number = 0, isPublic: boolean = false): string => {
    console.log('添加上传任务到store:', file.name, file.size)
    
    const taskId = generateTaskId()
    const newTask: UploadTask = {
      id: taskId,
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
      abortController: new AbortController(),
      createTime: Date.now(),
      failedChunks: new Set<number>(),
      retryCount: new Map<number, number>()
    }
    
    tasks.value.push(newTask)
    console.log('任务已添加到store，当前任务列表:', tasks.value)
    
    // 开始上传任务
    startUploadTask(taskId)
    
    return taskId
  }

  // 并发控制函数
  const concurrentUpload = async (tasks: (() => Promise<any>)[], concurrency: number = 3): Promise<void> => {
    const executing: Promise<any>[] = []
    
    for (const task of tasks) {
      const promise = task().then(() => {
        executing.splice(executing.indexOf(promise), 1)
      }).catch((error) => {
        executing.splice(executing.indexOf(promise), 1)
        throw error
      })
      executing.push(promise)
      
      if (executing.length >= concurrency) {
        await Promise.race(executing)
      }
    }
    
    await Promise.all(executing)
  }

  // 上传单个分块（带重试机制）
  const uploadChunkWithRetry = async (task: UploadTask, chunkIndex: number, chunk: Blob, maxRetries: number = 3): Promise<void> => {
    const retryCount = task.retryCount.get(chunkIndex) || 0
    
    try {
      await fileApi.uploadChunk(task.uploadId, chunkIndex, chunk)
      
      // 上传成功，更新状态
      if (!task.uploadedChunks.includes(chunkIndex)) {
        task.uploadedChunks.push(chunkIndex)
        task.uploadedSize += chunk.size
        task.progress = Math.min((task.uploadedSize / task.file.size) * 100, 100)
      }
      
      // 清除失败记录
      task.failedChunks.delete(chunkIndex)
      task.retryCount.delete(chunkIndex)
      
      console.log(`分块 ${chunkIndex} 上传完成，进度: ${task.progress.toFixed(2)}%`)
    } catch (error) {
      console.error(`分块 ${chunkIndex} 上传失败 (第${retryCount + 1}次尝试):`, error)
      
      if (retryCount < maxRetries) {
        // 记录重试次数并重新尝试
        task.retryCount.set(chunkIndex, retryCount + 1)
        task.failedChunks.add(chunkIndex)
        
        // 延迟重试，避免立即重试
        await new Promise(resolve => setTimeout(resolve, Math.pow(2, retryCount) * 1000))
        
        return uploadChunkWithRetry(task, chunkIndex, chunk, maxRetries)
      } else {
        // 超过最大重试次数，标记为失败
        task.failedChunks.add(chunkIndex)
        throw new Error(`分块 ${chunkIndex} 上传失败，已重试 ${maxRetries} 次`)
      }
    }
  }

  // 开始上传任务
  const startUploadTask = async (taskId: string) => {
    const task = tasks.value.find(t => t.id === taskId)
    if (!task || task.status !== 'uploading') return

    try {
      const CHUNK_SIZE = 6 * 1024 * 1024 // 6MB 分块大小
      const MAX_CONCURRENT_UPLOADS = 4 // 最大并发上传数
      
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
      
      // 准备需要上传的分块任务
      const uploadTasks: (() => Promise<any>)[] = []
      
      for (let i = 0; i < task.totalChunks; i++) {
        if (task.uploadedChunks.includes(i)) {
          continue
        }
        
        uploadTasks.push(async () => {
           // 如果任务状态不是上传中，则跳过
           if (task.status !== 'uploading') {
             return
           }
           
           // 计算分块范围
           const start = i * CHUNK_SIZE
           const end = Math.min(task.file.size, start + CHUNK_SIZE)
           const chunk = task.file.slice(start, end)
           
           // 使用带重试机制的上传方法
           await uploadChunkWithRetry(task, i, chunk)
         })
      }
      
      // 并发上传分块
      if (uploadTasks.length > 0) {
        console.log(`开始并发上传 ${uploadTasks.length} 个分块，最大并发数: ${MAX_CONCURRENT_UPLOADS}`)
        await concurrentUpload(uploadTasks, MAX_CONCURRENT_UPLOADS)
      }
      
      // 所有分块上传完成，合并文件
      if (task.uploadedChunks.length === task.totalChunks && task.status === 'uploading') {
        try {
          console.log('所有分块上传完成，开始合并文件')
          await fileApi.completeChunkUpload(task.uploadId, task.totalChunks)
          task.status = 'completed'
          task.progress = 100
          ElMessage.success(`文件 ${task.fileName} 上传成功`)
        } catch (completeError) {
          console.error('完成上传请求失败:', completeError)
          // 即使完成请求失败，文件可能已经上传成功
          // 设置为已完成状态，但显示警告信息
          task.status = 'completed'
          task.progress = 100
          ElMessage.warning(`文件 ${task.fileName} 上传完成，但服务器响应超时。请检查文件列表确认上传状态。`)
        }
      }
    } catch (error) {
      console.error('上传失败:', error)
      task.status = 'error'
      ElMessage.error(`文件 ${task.fileName} 上传失败`)
    }
  }

  // 暂停任务
  const pauseTask = (taskId: string) => {
    const task = tasks.value.find(t => t.id === taskId)
    if (!task) return
    
    task.status = 'paused'
    task.abortController?.abort()
    task.abortController = null
    ElMessage.info(`已暂停上传 ${task.fileName}`)
  }

  // 继续任务
  const resumeTask = (taskId: string) => {
    const task = tasks.value.find(t => t.id === taskId)
    if (!task) return
    
    task.status = 'uploading'
    task.abortController = new AbortController()
    
    // 确保新字段已初始化
    if (!task.failedChunks) {
      task.failedChunks = new Set<number>()
    }
    if (!task.retryCount) {
      task.retryCount = new Map<number, number>()
    }
    
    startUploadTask(taskId)
    ElMessage.success(`继续上传 ${task.fileName}`)
  }

  // 取消任务
  const cancelTask = (taskId: string) => {
    const taskIndex = tasks.value.findIndex(t => t.id === taskId)
    if (taskIndex === -1) return
    
    const task = tasks.value[taskIndex]
    task.abortController?.abort()
    tasks.value.splice(taskIndex, 1)
    ElMessage.info('已取消上传任务')
  }

  // 移除任务
  const removeTask = (taskId: string) => {
    const taskIndex = tasks.value.findIndex(t => t.id === taskId)
    if (taskIndex !== -1) {
      tasks.value.splice(taskIndex, 1)
    }
  }

  // 清除已完成任务
  const clearCompletedTasks = () => {
    tasks.value = tasks.value.filter(task => task.status !== 'completed')
    ElMessage.success('已清除所有已完成任务')
  }

  // 获取任务状态文本
  const getStatusText = (status: TaskStatus) => {
    switch (status) {
      case 'uploading': return '上传中'
      case 'paused': return '已暂停'
      case 'completed': return '已完成'
      case 'error': return '上传失败'
      default: return '未知状态'
    }
  }

  // 获取任务状态样式类
  const getStatusClass = (status: TaskStatus) => {
    switch (status) {
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

  // 获取上传统计信息
  const getUploadStats = (taskId: string) => {
    const task = tasks.value.find(t => t.id === taskId)
    if (!task) return null
    
    const successfulChunks = task.uploadedChunks.length
    const failedChunks = task.failedChunks?.size || 0
    const totalRetries = Array.from(task.retryCount?.values() || []).reduce((sum, count) => sum + count, 0)
    
    return {
      totalChunks: task.totalChunks,
      successfulChunks,
      failedChunks,
      pendingChunks: task.totalChunks - successfulChunks - failedChunks,
      totalRetries,
      successRate: task.totalChunks > 0 ? (successfulChunks / task.totalChunks * 100).toFixed(2) : '0.00'
    }
  }

  return {
    // 状态
    tasks,
    isUploading,
    
    // 计算属性
    hasCompletedTasks,
    uploadingTasks,
    completedTasks,
    
    // 方法
    addTask,
    startUploadTask,
    pauseTask,
    resumeTask,
    cancelTask,
    removeTask,
    clearCompletedTasks,
    getStatusText,
    getStatusClass,
    formatFileSize,
    getUploadStats
  }
})