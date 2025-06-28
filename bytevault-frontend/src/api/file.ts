import request from '@/utils/request'

/**
 * 上传文件
 * @param file 文件对象
 * @param parentId 父目录ID
 * @param isPublic 是否公开
 * @returns 上传结果
 */
export function uploadFile(file: File, parentId: number = 0, isPublic: boolean = false) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('parentId', parentId.toString())
  formData.append('isPublic', isPublic ? 'true' : 'false')
  
  return request({
    url: '/api/files/upload',
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
    url: `/api/files/${fileId}/download-url`,
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
    url: `/api/files/${fileId}/visibility`,
    method: 'put',
    data: {
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
    url: '/api/files/user',
    method: 'get',
    params: {
      parentId,
      page,
      pageSize
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
      pageSize
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
      pageSize
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
 * 导出所有API
 * @returns 所有API对象
 */
export const fileApi = {
  getUserFiles,
  getPublicFiles,
  uploadFile,
  createFolder,
  getFileDownloadUrl,
  deleteFile,
  updateFilePublicStatus,
  searchFiles,
  getFileInfo
} 