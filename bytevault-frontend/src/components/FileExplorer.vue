<template>
  <div class="file-explorer">
    <!-- 文件操作栏 -->
    <div class="file-actions">
      <div class="left-actions">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">
            <i class="el-icon-house"></i> 首页
          </el-breadcrumb-item>
          <el-breadcrumb-item>
            <span class="wiggle">{{ title }}</span>
          </el-breadcrumb-item>
          <template v-if="currentDirectory.id !== 0">
            <el-breadcrumb-item @click="navigateToParent" class="clickable-breadcrumb">
              <span class="wiggle">{{ currentDirectory.name }}</span>
            </el-breadcrumb-item>
          </template>
        </el-breadcrumb>
      </div>

      <div class="right-actions">
        <div class="search-and-buttons">
          <el-input v-model="searchKeyword" placeholder="搜索文件" class="search-input" @keyup.enter="handleSearch">
            <template #append>
              <el-button @click="handleSearch">
                <el-icon>
                  <Search />
                </el-icon>
              </el-button>
            </template>
          </el-input>

          <div class="buttons-group">
            <el-button type="info" @click="navigateToUploadTasks" class="action-button wiggle">
              <el-icon>
                <List />
              </el-icon>
              上传任务列表
            </el-button>

            <el-button v-if="showUpload" type="primary" @click="showChunkUploadDialog = true" class="action-button wiggle">
              <el-icon>
                <Upload />
              </el-icon>
              上传文件
            </el-button>

            <el-button v-if="showUpload" type="warning" @click="triggerFolderUpload" class="action-button wiggle">
              <el-icon>
                <FolderAdd />
              </el-icon>
              上传文件夹
            </el-button>
          </div>
        </div>

        <!-- 隐藏的文件夹上传输入 -->
        <input ref="folderInput" type="file" @change="handleFolderUpload" webkitdirectory directory multiple
          style="display: none" />
      </div>
    </div>

    <!-- 文件列表 -->
    <el-card class="file-list-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="card-title">{{ title }}</span>
            <span v-if="isSearching" class="search-result-text">搜索结果: {{ searchKeyword }}</span>
          </div>
          <div class="header-right" v-if="currentDirectory.id !== 0">
            <span class="directory-path">
              当前目录: {{ currentDirectory.name }}
              <el-button size="small" @click="navigateToParent" type="text" class="wiggle">
                返回上级
              </el-button>
            </span>
          </div>
        </div>
      </template>

      <div v-if="loading" class="cute-loading-container">
        <div class="cute-loading"></div>
        <div class="loading-text">加载中...</div>
      </div>

      <el-empty v-else-if="fileList.length === 0" description="暂无文件" class="empty-files">
        <img src="@/assets/cute.jpeg" class="empty-image floating" alt="暂无文件" />
      </el-empty>

      <el-table v-else v-loading="loading" :data="fileList" style="width: 100%">
        <el-table-column label="文件名" prop="filename" min-width="200">
          <template #default="scope">
            <div class="file-name-cell">
              <el-icon v-if="scope.row.isDir" class="file-icon folder-icon">
                <Folder />
              </el-icon>
              <el-icon v-else class="file-icon">
                <Document />
              </el-icon>
              <span :class="{ 'folder-name': scope.row.isDir }"
                @click="scope.row.isDir ? navigateToFolder(scope.row) : handleDownload(scope.row)">
                {{ scope.row.filename }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="大小" prop="fileSize" width="100">
          <template #default="scope">
            {{ scope.row.isDir ? '-' : formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>

        <el-table-column label="所有者" prop="ownerName" width="100" />

        <el-table-column label="上传时间" prop="createTime" width="160">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="状态" prop="visibility" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.visibility === 'public' ? 'success' : 'info'" class="status-tag no-after">
              {{ scope.row.visibility === 'public' ? '公开' : '私有' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <div class="file-operations">
              <el-button 
                size="small" 
                @click="handleDownload(scope.row)"
                v-if="!scope.row.isDir"
                class="action-btn wiggle"
              >
                下载
              </el-button>
              <el-button 
                v-if="scope.row.userId === userStore.userInfo?.id"
                size="small" 
                type="danger" 
                @click="handleDelete(scope.row)"
                class="action-btn wiggle"
              >
                删除
              </el-button>
              <el-button
                v-if="scope.row.userId === userStore.userInfo?.id"
                size="small"
                type="info"
                @click="handleTogglePublic(scope.row)"
                class="action-btn wiggle visibility-btn"
              >
                {{ scope.row.visibility === 'public' ? '设为私有' : '设为公开' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <!-- 断点续传对话框 -->
    <el-dialog v-model="showChunkUploadDialog" title="文件上传" width="500px">
      <div class="chunk-upload-container">
        <el-upload
          class="chunk-upload"
          drag
          action="#"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleFileSelected"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处或 <em>点击选择</em>
          </div>
        </el-upload>

        <div v-if="selectedFile" class="selected-file-info">
          <div class="file-info-row">
            <span class="file-label">文件名:</span>
            <span class="file-value">{{ selectedFile.name }}</span>
          </div>
          <div class="file-info-row">
            <span class="file-label">大小:</span>
            <span class="file-value">{{ formatFileSize(selectedFile.size) }}</span>
          </div>
          <div class="file-info-row">
            <span class="file-label">类型:</span>
            <span class="file-value">{{ selectedFile.type || '未知' }}</span>
          </div>
        </div>

        <div class="upload-controls">
          <el-button 
            type="primary" 
            @click="startChunkUpload" 
            :disabled="!selectedFile || isUploading"
            class="wiggle"
          >
            开始上传
          </el-button>
          <el-button 
            type="danger" 
            @click="cancelUpload"
            :disabled="!selectedFile"
            class="wiggle"
          >
            取消
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onBeforeUnmount, inject, type Ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Search, Upload, Folder, FolderAdd, UploadFilled, List } from '@element-plus/icons-vue'
import { fileApi } from '@/api'
import type { UploadRequestOptions } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'

// 定义接口
interface UploadTaskService {
  tasks: any[]
  addTask: ((file: File, parentId: number, isPublic: boolean) => number) | null
}

// 定义组件属性
const props = defineProps({
  // 文件浏览器类型: 'my-files' 或 'public-files'
  type: {
    type: String,
    required: true,
    validator: (value: string) => ['my-files', 'public-files'].includes(value)
  },
  // 组件标题
  title: {
    type: String,
    default: '文件'
  }
})

// 定义事件
const emit = defineEmits(['update:loading'])

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

// 文件管理相关数据
const currentDirectory = ref({ id: 0, name: '根目录' })
const fileList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const isSearching = ref(false)

// 计算属性
const showUpload = computed(() => props.type === 'my-files')
const showCreateFolder = computed(() => props.type === 'my-files')

// 文件夹上传相关
const folderInput = ref<HTMLInputElement | null>(null)

// 断点续传相关
const CHUNK_SIZE = 6 * 1024 * 1024 // 6MB 分块大小，确保大于MinIO的5MB最小要求
const showChunkUploadDialog = ref(false)
const selectedFile = ref<File | null>(null)
const isUploading = ref(false)
const isPaused = ref(false)
const currentUploadId = ref('')
const uploadedChunks = ref<number[]>([])
const totalChunks = ref(0)
const uploadController = ref<AbortController | null>(null)

// 监听浏览器历史记录变化
const handlePopState = (event: PopStateEvent) => {
  if (event.state && event.state.directoryId !== undefined) {
    const historyState = event.state
    currentDirectory.value = { 
      id: historyState.directoryId, 
      name: historyState.directoryName || '根目录' 
    }
    loadFiles()
  }
}

// 添加和移除popstate事件监听器
onMounted(() => {
  window.addEventListener('popstate', handlePopState)
  
  // 初始化历史状态
  if (!window.history.state) {
    window.history.replaceState(
      { directoryId: 0, directoryName: '根目录' }, 
      '', 
      window.location.pathname
    )
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('popstate', handlePopState)
})

// 监听类型变化，重置状态并加载文件
watch(() => props.type, (newType) => {
  console.log('文件浏览器类型改变:', newType)
  resetState()
  
  // 延时加载文件列表，确保DOM更新完成
  setTimeout(() => {
    loadFiles()
  }, 50)
})

// 重置状态
const resetState = () => {
  currentDirectory.value = { id: 0, name: '根目录' }
  currentPage.value = 1
  isSearching.value = false
  searchKeyword.value = ''
  
  // 更新历史记录
  window.history.replaceState(
    { directoryId: 0, directoryName: '根目录' }, 
    '', 
    window.location.pathname
  )
}

// 加载文件列表
const loadFiles = async () => {
  // 强制清空当前文件列表，确保UI立即更新
  fileList.value = []
  
  loading.value = true
  emit('update:loading', true)

  try {
    let response: any

    if (isSearching.value && searchKeyword.value) {
      // 搜索文件
      response = await fileApi.searchFiles(searchKeyword.value, currentPage.value, pageSize.value)
    } else if (props.type === 'my-files') {
      // 加载用户文件
      response = await fileApi.getUserFiles(currentDirectory.value.id, currentPage.value, pageSize.value)
    } else {
      // 加载公共文件
      response = await fileApi.getPublicFiles(currentPage.value, pageSize.value)
    }
    console.log('加载文件响应:', response)
    if (response) {
      fileList.value = response.files || []
      total.value = response.total || 0
    } else {
      fileList.value = []
      total.value = 0
      ElMessage.warning('未获取到文件数据')
    }
  } catch (error) {
    console.error('加载文件失败:', error)
    ElMessage.error('加载文件失败，请稍后重试')
    fileList.value = []
    total.value = 0
  } finally {
    loading.value = false
    emit('update:loading', false)
  }
}

// 自定义文件上传
const customUpload = async (options: UploadRequestOptions) => {
  // 获取文件
  const file = options.file as File
  selectedFile.value = file
  
  // 获取全局上传任务服务
  const uploadTaskService = inject<Ref<UploadTaskService>>('uploadTaskService')
  
  // 如果存在全局上传任务服务，则添加任务
  if (uploadTaskService?.value?.addTask) {
    uploadTaskService.value.addTask(file, currentDirectory.value.id, false)
    ElMessage.success('已添加到上传任务列表')
    showChunkUploadDialog.value = false
    resetChunkUpload()
    
    // 跳转到上传任务列表页面
    router.push('/upload-tasks')
    return
  }
  
  // 直接开始上传
  await startChunkUpload()
}

// 处理文件下载
const handleDownload = async (file: any) => {
  try {
    // 如果是目录，则进入该目录
    if (file.isDir) {
      navigateToFolder(file)
      return
    }

    loading.value = true
    emit('update:loading', true)

    // 使用fileApi提供的方法下载文件，确保正确携带token
    const response = await fileApi.downloadFileDirectly(file.id)
    
    // 创建Blob URL并触发下载
    const url = window.URL.createObjectURL(response)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', file.filename)
    document.body.appendChild(link)
    link.click()
    
    // 清理
    setTimeout(() => {
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    }, 100)
    
    ElMessage.success('文件下载开始')
  } catch (error) {
    console.error('文件下载失败:', error)
    ElMessage.error('文件下载失败，请稍后重试')
  } finally {
    loading.value = false
    emit('update:loading', false)
  }
}

// 导航到文件夹
const navigateToFolder = (folder: any) => {
  // 保存当前状态到历史记录
  window.history.pushState(
    { directoryId: folder.id, directoryName: folder.filename }, 
    '', 
    window.location.pathname
  )
  
  currentDirectory.value = { id: folder.id, name: folder.filename }
  currentPage.value = 1
  loadFiles()
}

// 返回上级目录
const navigateToParent = async () => {
  if (currentDirectory.value.id === 0) {
    return
  }

  try {
    loading.value = true
    emit('update:loading', true)

    // 获取当前文件夹信息
    const response = await fileApi.getFileInfo(currentDirectory.value.id) as any

    if (response && response.file) {
      const parentId = response.file.parentId || 0
      let parentName = '根目录'

      if (parentId !== 0) {
        // 获取父文件夹信息
        const parentResponse = await fileApi.getFileInfo(parentId) as any
        if (parentResponse && parentResponse.file) {
          parentName = parentResponse.file.filename
        }
      }
      
      // 更新历史记录
      window.history.pushState(
        { directoryId: parentId, directoryName: parentName }, 
        '', 
        window.location.pathname
      )

      currentDirectory.value = { id: parentId, name: parentName }
      currentPage.value = 1
      loadFiles()
    } else {
      window.history.pushState(
        { directoryId: 0, directoryName: '根目录' }, 
        '', 
        window.location.pathname
      )
      
      currentDirectory.value = { id: 0, name: '根目录' }
      currentPage.value = 1
      loadFiles()
      ElMessage.warning('获取文件夹信息失败，已返回根目录')
    }
  } catch (error) {
    console.error('导航失败:', error)
    ElMessage.error('导航失败，已返回根目录')
    
    window.history.pushState(
      { directoryId: 0, directoryName: '根目录' }, 
      '', 
      window.location.pathname
    )
    
    currentDirectory.value = { id: 0, name: '根目录' }
    currentPage.value = 1
    loadFiles()
  } finally {
    loading.value = false
    emit('update:loading', false)
  }
}

// 创建文件夹
const createNewFolder = async () => {
  try {
    const { value: folderName } = await ElMessageBox.prompt('请输入文件夹名称', '创建文件夹', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputValidator: (value) => {
        if (!value) {
          return '文件夹名称不能为空'
        }
        if (value.length > 50) {
          return '文件夹名称不能超过50个字符'
        }
        return true
      }
    })

    if (folderName) {
      loading.value = true
      emit('update:loading', true)

      const response = await fileApi.createFolder(folderName, currentDirectory.value.id)

      if (response && response.data) {
        ElMessage.success('文件夹创建成功')
        loadFiles()
      } else {
        ElMessage.error('文件夹创建失败')
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('创建文件夹失败:', error)
      ElMessage.error('创建文件夹失败')
    }
  } finally {
    loading.value = false
    emit('update:loading', false)
  }
}

// 处理文件删除
const handleDelete = async (file: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除${file.isDir ? '文件夹' : '文件'} "${file.filename}" 吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    loading.value = true
    emit('update:loading', true)

    await fileApi.deleteFile(file.id)
    ElMessage.success(`${file.isDir ? '文件夹' : '文件'}删除成功`)
    loadFiles() // 重新加载文件列表
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  } finally {
    loading.value = false
    emit('update:loading', false)
  }
}

// 处理切换文件公开状态
const handleTogglePublic = async (file: any) => {
  try {
    loading.value = true
    
    const isPublic = file.visibility !== 'public'
    let response
    
    if (file.isDir) {
      // 如果是文件夹，调用文件夹公开状态更新API
      response = await fileApi.updateFolderPublicStatus(file.id, isPublic)
      ElMessage.success('文件夹及其子文件公开状态已更新')
    } else {
      // 如果是文件，调用文件公开状态更新API
      response = await fileApi.updateFilePublicStatus(file.id, isPublic)
      ElMessage.success('文件公开状态已更新')
    }
    
    // 更新本地状态
    file.visibility = isPublic ? 'public' : 'private'
  } catch (error) {
    console.error('更新公开状态失败', error)
    ElMessage.error('更新公开状态失败')
  } finally {
    loading.value = false
  }
}

// 处理搜索
const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    isSearching.value = true
    currentPage.value = 1
    loadFiles()
  } else {
    isSearching.value = false
    loadFiles()
  }
}

// 处理分页大小变化
const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadFiles()
}

// 处理页码变化
const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadFiles()
}

// 格式化文件大小
const formatFileSize = (size: number) => {
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let index = 0
  let convertedSize = size
  
  // 当文件大小大于1024且单位还没到TB时，继续转换
  while (convertedSize >= 1024 && index < units.length - 1) {
    convertedSize /= 1024
    index++
  }
  
  // 根据大小决定小数点位数
  // 对于B不显示小数点，KB保留1位小数，MB及以上保留2位小数
  let decimalPlaces = 0
  if (index === 1) decimalPlaces = 1      // KB保留1位小数
  else if (index >= 2) decimalPlaces = 2  // MB及以上保留2位小数
  
  // 格式化数字并添加单位
  return convertedSize.toFixed(decimalPlaces).replace(/\.0+$/, '') + ' ' + units[index]
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  
  // 获取年月日时分
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  
  // 返回格式化后的日期字符串：YYYY-MM-DD HH:MM
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 触发文件夹上传
const triggerFolderUpload = () => {
  folderInput.value?.click()
}

// 处理文件夹上传
const handleFolderUpload = async (event: Event) => {
  const input = event.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return

  loading.value = true
  emit('update:loading', true)

  try {
    const files = Array.from(input.files)
    const relativePaths: string[] = []

    // 获取所有文件的相对路径
    for (const file of files) {
      // 在Chrome中，webkitRelativePath属性包含文件的相对路径
      const path = (file as any).webkitRelativePath
      relativePaths.push(path)
    }

    // 调用上传API
    const response = await fileApi.uploadFolder(files, relativePaths, currentDirectory.value.id, false)

    ElMessage.success('文件夹上传成功')
    loadFiles() // 重新加载文件列表
  } catch (error: any) {
    console.error('文件夹上传失败', error)
    ElMessage.error(`文件夹上传失败: ${error.message || '未知错误'}`)
  } finally {
    loading.value = false
    emit('update:loading', false)
    // 重置输入框，以便可以再次选择同一文件夹
    if (input) input.value = ''
  }
}

// 处理文件选择
const handleFileSelected = (file: any) => {
  selectedFile.value = file.raw
  isUploading.value = false
  isPaused.value = false
}

// 取消上传
const cancelUpload = () => {
  resetChunkUpload()
  showChunkUploadDialog.value = false
}

// 开始分块上传
const startChunkUpload = async () => {
  if (!selectedFile.value) return
  
  // 获取全局上传任务服务
  const uploadTaskService = inject<Ref<UploadTaskService>>('uploadTaskService')
  
  // 如果存在全局上传任务服务，则添加任务
  if (uploadTaskService?.value?.addTask) {
    uploadTaskService.value.addTask(selectedFile.value, currentDirectory.value.id, false)
    ElMessage.success('已添加到上传任务列表')
    showChunkUploadDialog.value = false
    resetChunkUpload()
    
    // 跳转到上传任务列表页面
    router.push('/upload-tasks')
    return
  }
  
  // 如果没有全局上传任务服务，则在当前组件中处理上传
  try {
    // 关闭上传对话框
    showChunkUploadDialog.value = false
    
    isUploading.value = true
    isPaused.value = false
    uploadController.value = new AbortController()
    
    // 初始化上传
    const initResponse: any = await fileApi.initChunkUpload(
      selectedFile.value.name,
      selectedFile.value.size,
      selectedFile.value.type,
      currentDirectory.value.id,
      false // 默认私有
    )
    
    if (!initResponse || !initResponse.uploadId) {
      throw new Error('初始化上传失败')
    }
    
    currentUploadId.value = initResponse.uploadId
    
    // 获取已上传的分块列表
    const chunksResponse: any = await fileApi.getUploadedChunks(currentUploadId.value)
    uploadedChunks.value = chunksResponse?.uploadedChunks || []
    
    // 计算总分块数
    totalChunks.value = Math.ceil(selectedFile.value.size / CHUNK_SIZE)
    
    // 开始上传分块
    await uploadChunks()
    
  } catch (error) {
    console.error('开始上传失败:', error)
    ElMessage.error('开始上传失败')
    isUploading.value = false
  }
}

// 上传分块
const uploadChunks = async () => {
  if (!selectedFile.value || !currentUploadId.value) return
  
  try {
    const file = selectedFile.value
    const chunks = Math.ceil(file.size / CHUNK_SIZE)
    
    for (let i = 0; i < chunks; i++) {
      // 如果上传已取消，则退出循环
      if (!isUploading.value) {
        return
      }
      
      if (uploadedChunks.value.includes(i)) {
        continue
      }
      
      // 计算分块范围
      const start = i * CHUNK_SIZE
      const end = Math.min(file.size, start + CHUNK_SIZE)
      const chunk = file.slice(start, end)
      
      // 上传分块
      await fileApi.uploadChunk(currentUploadId.value, i, chunk)
      
      // 更新已上传分块
      uploadedChunks.value.push(i)
    }
    
    // 所有分块上传完成，合并文件
    if (uploadedChunks.value.length === chunks) {
      const completeResponse = await fileApi.completeChunkUpload(currentUploadId.value, chunks)
      ElMessage.success('文件上传成功')
      resetChunkUpload()
      loadFiles() // 重新加载文件列表
    }
  } catch (error) {
    console.error('上传分块失败:', error)
    ElMessage.error('上传分块失败')
    isUploading.value = false
  }
}

// 重置上传状态
const resetChunkUpload = () => {
  selectedFile.value = null
  isUploading.value = false
  isPaused.value = false
  currentUploadId.value = ''
  uploadedChunks.value = []
  totalChunks.value = 0
  uploadController.value = null
}

// 导航到上传任务列表页面
const navigateToUploadTasks = () => {
  router.push('/upload-tasks')
}

// 组件挂载时加载文件列表
onMounted(() => {
  loadFiles()
})

// 暴露方法给父组件
defineExpose({
  loadFiles,
  resetState
})
</script>

<style scoped>
.file-explorer {
  width: 100%;
}

.file-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  flex-wrap: wrap;
  gap: 10px;
}

.left-actions {
  display: flex;
  align-items: center;
}

.right-actions {
  display: flex;
  align-items: center;
}

.search-and-buttons {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.search-input {
  width: 220px;
}

.buttons-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-button {
  white-space: nowrap;
}

.file-list-card {
  margin-bottom: 20px;
  transition: all 0.3s ease;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.header-left, .header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title {
  font-weight: bold;
  font-size: 1.2em;
  color: var(--primary-color);
  white-space: nowrap;
}

.file-name-cell {
  display: flex;
  align-items: center;
}

.file-name-cell .el-icon {
  margin-right: 8px;
  font-size: 1.2em;
}

.folder-name {
  color: var(--secondary-color);
  cursor: pointer;
  font-weight: bold;
  transition: all 0.2s ease;
}

.folder-name:hover {
  text-decoration: underline;
  transform: translateX(3px);
}

.directory-path {
  font-size: 0.9em;
  color: var(--light-text);
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.clickable-breadcrumb {
  cursor: pointer;
  color: var(--secondary-color);
  transition: all 0.2s ease;
}

.clickable-breadcrumb:hover {
  color: var(--primary-color);
  text-decoration: underline;
}

.pagination-container {
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
}

.status-tag {
  font-weight: bold;
}

/* 使用深度选择器覆盖el-tag的样式，移除小圆点 */
:deep(.status-tag .el-tag__content::after) {
  display: none !important;
}

:deep(.el-tag__content::after) {
  display: none !important;
}

.empty-files {
  padding: 30px 0;
}

.empty-image {
  width: 150px;
  margin-bottom: 15px;
}

.cute-loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 0;
}

.loading-text {
  margin-top: 15px;
  color: var(--primary-color);
  font-weight: bold;
  font-size: 1.1em;
}

.search-result-text {
  background-color: rgba(255, 105, 180, 0.15);
  padding: 3px 10px;
  border-radius: 20px;
  color: var(--primary-color);
  font-weight: bold;
  font-size: 0.9em;
}

.file-operations {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  justify-content: flex-start;
}

.action-btn {
  padding: 4px 8px;
  font-size: 12px;
}

.visibility-btn {
  min-width: 80px;
}

@media (max-width: 768px) {
  .file-actions {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .right-actions {
    width: 100%;
  }
  
  .search-and-buttons {
    flex-direction: column;
    align-items: flex-start;
    width: 100%;
  }
  
  .search-input {
    width: 100%;
  }
  
  .buttons-group {
    margin-top: 10px;
    width: 100%;
    justify-content: space-between;
  }
}

.chunk-upload-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.selected-file-info {
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 12px;
  margin-top: 10px;
}

.file-info-row {
  display: flex;
  margin-bottom: 5px;
}

.file-label {
  font-weight: bold;
  width: 80px;
}

.file-value {
  flex: 1;
  word-break: break-all;
}

.upload-progress-container {
  margin: 15px 0;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
  font-size: 14px;
  color: #606266;
}

.kawaii-progress-bar {
  position: relative;
  height: 30px;
  margin-top: 20px;
}

.progress-track {
  height: 10px;
  background-color: #e9ecef;
  border-radius: 5px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff9a9e 0%, #fad0c4 99%, #fad0c4 100%);
  border-radius: 5px;
  transition: width 0.3s ease;
}

.kawaii-character {
  position: absolute;
  bottom: 5px;
  transform: translateX(-50%);
  transition: left 0.3s ease;
}

.character-face {
  width: 30px;
  height: 30px;
  background-color: #ffd6e0;
  border-radius: 50%;
  position: relative;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  display: flex;
  justify-content: center;
  align-items: center;
}

.eyes {
  width: 16px;
  height: 6px;
  position: relative;
  top: -2px;
}

.eyes:before, .eyes:after {
  content: '';
  position: absolute;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background-color: #333;
  top: 0;
}

.eyes:before {
  left: 0;
}

.eyes:after {
  right: 0;
}

.mouth {
  width: 8px;
  height: 3px;
  background-color: #333;
  border-radius: 2px;
  position: relative;
  top: 3px;
}

.happy-mouth {
  height: 6px;
  border-radius: 50% 50% 0 0;
  transform: rotate(180deg);
}

.upload-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}
</style>