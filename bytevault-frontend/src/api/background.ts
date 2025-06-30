import request from '@/utils/request'

/**
 * 上传背景图片
 * @param file 图片文件
 * @returns 上传结果
 */
export function uploadBackgroundImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: '/api/users/background',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取用户的所有背景图片
 * @returns 背景图片列表
 */
export function getUserBackgroundImages() {
  return request({
    url: '/api/users/background/my',
    method: 'get'
  })
}

/**
 * 删除背景图片
 * @param imageId 图片ID
 * @returns 删除结果
 */
export function deleteBackgroundImage(imageId: number) {
  return request({
    url: `/api/users/background/${imageId}`,
    method: 'delete'
  })
}

/**
 * 设置当前背景图片
 * @param imageId 图片ID
 * @returns 设置结果
 */
export function setCurrentBackgroundImage(imageId: number) {
  return request({
    url: `/api/users/background/${imageId}/set-current`,
    method: 'put'
  })
}

/**
 * 获取当前背景图片
 * @returns 当前背景图片URL
 */
export function getCurrentBackgroundImage() {
  return request({
    url: '/api/users/background/current',
    method: 'get'
  })
}

/**
 * 导出所有API
 */
export const backgroundApi = {
  uploadBackgroundImage,
  getUserBackgroundImages,
  deleteBackgroundImage,
  setCurrentBackgroundImage,
  getCurrentBackgroundImage
} 