import request from '@/utils/request'
import axios from 'axios'

/**
 * 上传文件 (使用断点续传实现)
 * @param file 文件对象
 * @param parentId 父目录ID
 * @param isPublic 是否公开
 * @returns 上传结果
 */
export async function uploadFile(file: File, parentId: number = 0, isPublic: boolean = false) {
  // 使用断点续传方式上传文件
  const CHUNK_SIZE = 6 * 1024 * 1024 // 6MB 分块大小，确保大于MinIO的5MB最小要求
  
  // 初始化上传
  const initResponse: any = await initChunkUpload(
    file.name,
    file.size,
    file.type,
    parentId,
    isPublic
  )
  
  if (!initResponse || !initResponse.uploadId) {
    throw new Error('初始化上传失败')
  }
  
  const uploadId = initResponse.uploadId
  
  // 获取已上传的分块列表
  const chunksResponse: any = await getUploadedChunks(uploadId)
  const uploadedChunks = chunksResponse?.uploadedChunks || []
  
  // 计算总分块数
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
  
  // 上传分块
  for (let i = 0; i < totalChunks; i++) {
    // 如果分块已上传，则跳过
    if (uploadedChunks.includes(i)) {
      continue
    }
    
    // 计算分块的起始和结束位置
    const start = i * CHUNK_SIZE
    const end = Math.min(file.size, start + CHUNK_SIZE)
    const chunk = file.slice(start, end)
    
    // 上传分块
    await uploadChunk(uploadId, i, chunk)
  }
  
  // 完成上传
  const completeResponse = await completeChunkUpload(uploadId, totalChunks)
  return completeResponse
}

/**
 * 初始化分块上传
 * @param filename 文件名
 * @param fileSize 文件大小
 * @param fileType 文件类型
 * @param parentId 父目录ID
 * @param isPublic 是否公开
 * @returns 上传ID和其他初始化信息
 */
export function initChunkUpload(filename: string, fileSize: number, fileType: string, parentId: number = 0, isPublic: boolean = false) {
  return request({
    url: '/api/files/chunk/init',
    method: 'post',
    data: {
      filename,
      fileSize,
      fileType,
      parentId,
      isPublic
    }
  })
}

/**
 * 上传分块
 * @param uploadId 上传ID
 * @param chunkIndex 分块索引
 * @param chunk 分块数据
 * @returns 上传结果
 */
export function uploadChunk(uploadId: string, chunkIndex: number, chunk: Blob) {
  const formData = new FormData()
  formData.append('uploadId', uploadId)
  formData.append('chunkIndex', chunkIndex.toString())
  formData.append('chunk', chunk)
  
  return request({
    url: `/api/files/chunk/upload`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取已上传的分块列表
 * @param uploadId 上传ID
 * @returns 已上传的分块索引列表
 */
export function getUploadedChunks(uploadId: string) {
  return request({
    url: `/api/files/chunk/uploaded/${uploadId}`,
    method: 'get'
  })
}

/**
 * 完成分块上传
 * @param uploadId 上传ID
 * @param totalChunks 总分块数
 * @returns 完成上传的结果
 */
export function completeChunkUpload(uploadId: string, totalChunks: number) {
  return request({
    url: `/api/files/chunk/complete/${uploadId}`,
    method: 'post',
    data: {
      totalChunks
    },
    timeout: 60000 // 设置60秒超时，因为合并大文件可能需要较长时间
  })
}

/**
 * 上传文件夹
 * @param files 文件列表
 * @param relativePaths 文件相对路径列表
 * @param parentId 父目录ID
 * @param isPublic 是否公开
 * @returns 上传结果
 */
export function uploadFolder(files: File[], relativePaths: string[], parentId: number = 0, isPublic: boolean = false) {
  const formData = new FormData()
  
  // 添加每个文件和对应的相对路径
  files.forEach((file, index) => {
    formData.append('files', file)
    formData.append('relativePaths', relativePaths[index])
  })
  
  formData.append('parentId', parentId.toString())
  formData.append('isPublic', isPublic ? 'true' : 'false')
  
  return request({
    url: '/api/files/upload-folder',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 删除文件
 * @param fileId 文件ID
 * @returns 删除结果
 */
export function deleteFile(fileId: number) {
  return request({
    url: `/api/files/${fileId}`,
    method: 'delete'
  })
}

/**
 * 获取文件下载链接
 * @param fileId 文件ID
 * @returns 下载链接
 */
export function getFileDownloadUrl(fileId: number) {
  return request({
    url: `/api/files/${fileId}/download`,
    method: 'get'
  })
}

/**
 * 更新文件公开状态
 * @param fileId 文件ID
 * @param isPublic 是否公开
 * @returns 更新结果
 */
export function updateFilePublicStatus(fileId: number, isPublic: boolean) {
  return request({
    url: `/api/files/${fileId}/public`,
    method: 'put',
    params: {
      isPublic
    }
  })
}

/**
 * 获取用户文件列表
 * @param parentId 父目录ID
 * @param page 页码
 * @param pageSize 每页大小
 * @returns 文件列表
 */
export function getUserFiles(parentId: number = 0, page: number = 1, pageSize: number = 10) {
  return request({
    url: '/api/files/my',
    method: 'get',
    params: {
      parentId,
      page,
      size: pageSize
    }
  })
}

/**
 * 获取公开文件列表
 * @param page 页码
 * @param pageSize 每页大小
 * @returns 公开文件列表
 */
export function getPublicFiles(page: number = 1, pageSize: number = 10) {
  return request({
    url: '/api/files/public',
    method: 'get',
    params: {
      page,
      size: pageSize
    }
  })
}

/**
 * 搜索文件
 * @param keyword 关键词
 * @param page 页码
 * @param pageSize 每页大小
 * @returns 搜索结果
 */
export function searchFiles(keyword: string, page: number = 1, pageSize: number = 10) {
  return request({
    url: '/api/files/search',
    method: 'get',
    params: {
      keyword,
      page,
      size: pageSize
    }
  })
}

/**
 * 获取文件详情
 * @param fileId 文件ID
 * @returns 文件详情
 */
export function getFileInfo(fileId: number) {
  return request({
    url: `/api/files/${fileId}`,
    method: 'get'
  })
}

/**
 * 创建文件夹
 * @param folderName 文件夹名称
 * @param parentId 父目录ID
 * @returns 创建结果
 */
export function createFolder(folderName: string, parentId: number = 0) {
  return request({
    url: '/api/files/folder',
    method: 'post',
    data: {
      folderName,
      parentId
    }
  })
}

/**
 * 更新文件夹公开状态（包括所有子文件）
 * @param folderId 文件夹ID
 * @param isPublic 是否公开
 * @returns 更新结果
 */
export function updateFolderPublicStatus(folderId: number, isPublic: boolean) {
  return request({
    url: `/api/files/${folderId}/folder-public`,
    method: 'put',
    params: {
      isPublic
    }
  })
}

/**
 * 直接下载文件（返回Blob对象）
 * @param fileId 文件ID
 * @returns Blob对象
 */
export function downloadFileDirectly(fileId: number) {
  const token = localStorage.getItem('token')
  return axios.get(`/api/files/proxy/${fileId}`, {
    responseType: 'blob',
    headers: {
      'Authorization': token ? `Bearer ${token}` : ''
    }
  }).then(response => response.data)
}

/**
 * 导出所有API
 * @returns 所有API对象
 */
export const fileApi = {
  getUserFiles,
  getPublicFiles,
  uploadFile,
  uploadFolder,
  createFolder,
  getFileDownloadUrl,
  deleteFile,
  updateFilePublicStatus,
  updateFolderPublicStatus,
  searchFiles,
  getFileInfo,
  downloadFileDirectly,
  // 断点续传相关API
  initChunkUpload,
  uploadChunk,
  getUploadedChunks,
  completeChunkUpload
}