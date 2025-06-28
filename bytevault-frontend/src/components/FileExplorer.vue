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
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文件"
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
        
        <el-button
          v-if="showCreateFolder"
          type="success"
          @click="createNewFolder"
          class="action-button wiggle"
        >
          <el-icon><Folder /></el-icon>
          新建文件夹
        </el-button>
        
        <el-upload
          v-if="showUpload"
          class="upload-button"
          :show-file-list="false"
          :http-request="customUpload"
          :multiple="false"
        >
          <el-button type="primary" class="action-button wiggle">
            <el-icon><Upload /></el-icon>
            上传文件
          </el-button>
        </el-upload>
      </div>
    </div>
    
    <!-- 文件列表 -->
    <el-card class="file-list-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">{{ title }}</span>
          <span v-if="isSearching" class="search-result-text">搜索结果: {{ searchKeyword }}</span>
          <span v-if="currentDirectory.id !== 0" class="directory-path">
            当前目录: {{ currentDirectory.name }}
            <el-button size="small" @click="navigateToParent" type="text" class="wiggle">
              返回上级
            </el-button>
          </span>
        </div>
      </template>
      
      <div v-if="loading" class="cute-loading-container">
        <div class="cute-loading"></div>
        <div class="loading-text">加载中...</div>
      </div>
      
      <el-empty v-else-if="fileList.length === 0" description="暂无文件" class="empty-files">
        <img src="https://i.imgur.com/JXmxKxN.png" class="empty-image floating" alt="暂无文件" />
      </el-empty>
      
      <el-table
        v-else
        v-loading="loading"
        :data="fileList"
        style="width: 100%"
      >
        <el-table-column label="文件名" prop="filename" min-width="200">
          <template #default="scope">
            <div class="file-name-cell">
              <el-icon v-if="scope.row.isDir" class="file-icon folder-icon"><Folder /></el-icon>
              <el-icon v-else class="file-icon"><Document /></el-icon>
              <span 
                :class="{ 'folder-name': scope.row.isDir }"
                @click="scope.row.isDir ? navigateToFolder(scope.row) : handleDownload(scope.row)"
              >
                {{ scope.row.filename }}
              </span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="大小" prop="fileSize" width="120">
          <template #default="scope">
            {{ scope.row.isDir ? '-' : formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        
        <el-table-column label="所有者" prop="ownerName" width="120" />
        
        <el-table-column label="上传时间" prop="createTime" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="状态" prop="visibility" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.visibility === 'public' ? 'success' : 'info'" class="status-tag">
              {{ scope.row.visibility === 'public' ? '公开' : '私有' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
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
              v-if="scope.row.userId === userStore.userInfo?.id && !scope.row.isDir"
              size="small"
              type="info"
              @click="handleTogglePublic(scope.row)"
              class="action-btn wiggle"
            >
              {{ scope.row.visibility === 'public' ? '设为私有' : '设为公开' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Search, Upload, Folder } from '@element-plus/icons-vue'
import { fileApi } from '@/api'
import type { UploadRequestOptions } from 'element-plus'

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

// 监听类型变化，重置状态并加载文件
watch(() => props.type, () => {
  resetState()
  loadFiles()
})

// 重置状态
const resetState = () => {
  currentDirectory.value = { id: 0, name: '根目录' }
  currentPage.value = 1
  isSearching.value = false
  searchKeyword.value = ''
}

// 加载文件列表
const loadFiles = async () => {
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
    console.log(response)
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
  try {
    loading.value = true
    emit('update:loading', true)
    
    const file = options.file as File
    const isPublic = false // 默认上传为私有文件
    
    const response = await fileApi.uploadFile(file, currentDirectory.value.id, isPublic)
    console.log(response)
    ElMessage.success('文件上传成功')
    loadFiles() // 重新加载文件列表
  } catch (error) {
    console.error('文件上传失败:', error)
    ElMessage.error('文件上传失败')
  } finally {
    loading.value = false
    emit('update:loading', false)
  }
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
    
    const response = await fileApi.getFileDownloadUrl(file.id) as any
    console.log(response)
    if (response && response.downloadUrl) {
      // 创建临时链接并点击下载
      const link = document.createElement('a')
      link.href = response.downloadUrl
      link.setAttribute('download', file.filename)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
      ElMessage.success('文件下载开始')
    } else {
      ElMessage.error('获取下载链接失败')
    }
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
    const response = await fileApi.getFileInfo(currentDirectory.value.id)
    
    if (response && response.data && response.data.file) {
      const parentId = response.data.file.parentId || 0
      
      if (parentId === 0) {
        currentDirectory.value = { id: 0, name: '根目录' }
      } else {
        // 获取父文件夹信息
        const parentResponse = await fileApi.getFileInfo(parentId)
        if (parentResponse && parentResponse.data && parentResponse.data.file) {
          currentDirectory.value = { id: parentId, name: parentResponse.data.file.filename }
        } else {
          currentDirectory.value = { id: 0, name: '根目录' }
          ElMessage.warning('获取父文件夹信息失败，已返回根目录')
        }
      }
      
      currentPage.value = 1
      loadFiles()
    } else {
      currentDirectory.value = { id: 0, name: '根目录' }
      currentPage.value = 1
      loadFiles()
      ElMessage.warning('获取文件夹信息失败，已返回根目录')
    }
  } catch (error) {
    console.error('导航失败:', error)
    ElMessage.error('导航失败，已返回根目录')
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

// 处理文件公开/私有状态切换
const handleTogglePublic = async (file: any) => {
  try {
    const newStatus = file.visibility === 'public' ? false : true
    const statusText = newStatus ? '公开' : '私有'
    
    loading.value = true
    emit('update:loading', true)
    
    await fileApi.updateFilePublicStatus(file.id, newStatus)
    ElMessage.success(`文件已设为${statusText}`)
    loadFiles() // 重新加载文件列表
  } catch (error) {
    console.error('更新文件状态失败:', error)
    ElMessage.error('更新文件状态失败，请稍后重试')
  } finally {
    loading.value = false
    emit('update:loading', false)
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
  if (size < 1024) {
    return size + ' B'
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(2) + ' KB'
  } else if (size < 1024 * 1024 * 1024) {
    return (size / (1024 * 1024)).toFixed(2) + ' MB'
  } else {
    return (size / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
  }
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString()
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
  margin-bottom: 20px;
}

.search-input {
  width: 250px;
  margin-right: 15px;
}

.action-button {
  margin-left: 10px;
}

.file-list-card {
  margin-bottom: 20px;
  transition: all 0.3s ease;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-weight: bold;
  font-size: 1.2em;
  color: var(--primary-color);
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
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.status-tag {
  font-weight: bold;
}

.empty-files {
  padding: 40px 0;
}

.empty-image {
  width: 200px;
  margin-bottom: 20px;
}

.cute-loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 0;
}

.loading-text {
  margin-top: 20px;
  color: var(--primary-color);
  font-weight: bold;
  font-size: 1.2em;
}

.search-result-text {
  background-color: rgba(255, 105, 180, 0.1);
  padding: 4px 12px;
  border-radius: 20px;
  color: var(--primary-color);
  font-weight: bold;
}

.action-btn {
  margin: 0 3px;
}
</style> 