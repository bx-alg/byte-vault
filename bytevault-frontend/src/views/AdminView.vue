<template>
  <div class="admin-container">
    <div class="admin-header">
      <h1>用户管理</h1>
      <div class="admin-tools">
        <el-input
          v-model="searchQuery"
          placeholder="输入用户名搜索..."
          clearable
          @input="handleSearch"
          prefix-icon="el-icon-search"
        >
          <template #prefix>
            <el-icon><search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="refreshUserList">刷新</el-button>
        <el-button 
          type="danger" 
          :disabled="selectedUsers.length === 0" 
          @click="handleBatchStatusChange(0)"
        >
          批量禁用
        </el-button>
        <el-button 
          type="success" 
          :disabled="selectedUsers.length === 0" 
          @click="handleBatchStatusChange(1)"
        >
          批量启用
        </el-button>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="filteredUsers"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column label="角色" width="180">
        <template #default="{ row }">
          <el-tag 
            v-for="role in formatRoles(row.roles)" 
            :key="role"
            type="info"
            effect="plain"
            class="role-tag"
          >
            {{ role }}
          </el-tag>
          <el-button
            type="primary"
            size="small"
            link
            @click="handleEditRoles(row)"
          >
            编辑
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.updateTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button
            type="primary"
            size="small"
            @click="handleEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            v-if="row.status === 1"
            type="danger"
            size="small"
            @click="handleStatusChange(row, 0)"
          >
            禁用
          </el-button>
          <el-button
            v-else
            type="success"
            size="small"
            @click="handleStatusChange(row, 1)"
          >
            启用
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 用户信息编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑用户信息"
      width="30%"
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveUser">保存</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 用户角色编辑对话框 -->
    <el-dialog
      v-model="roleDialogVisible"
      title="编辑用户角色"
      width="30%"
    >
      <el-form :model="roleForm" label-width="80px">
        <el-form-item label="角色">
          <el-select
            v-model="roleForm.selectedRoles"
            multiple
            placeholder="请选择角色"
            style="width: 100%"
          >
            <el-option
              v-for="role in allRoles"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="roleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveRoles">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getAllUsers, getAllRoles, updateUser, assignRolesToUser, updateUserStatus, batchUpdateUserStatus } from '@/api/admin'
import { useUserStore } from '@/stores/user'
import type { User } from '@/api/admin'

const userStore = useUserStore()
const router = useRouter()

// 检查用户权限
if (!userStore.hasRole('admin')) {
  ElMessage.error('没有权限访问此页面')
  router.push('/')
}

// 状态数据
const users = ref<User[]>([])
const allRoles = ref<any[]>([])
const loading = ref(false)
const searchQuery = ref('')
const selectedUsers = ref<User[]>([])

// 编辑表单相关
const editDialogVisible = ref(false)
const editForm = ref({
  id: 0,
  username: ''
})

// 角色编辑相关
const roleDialogVisible = ref(false)
const roleForm = ref({
  userId: 0,
  selectedRoles: [] as number[]
})

// 格式化日期时间
const formatDateTime = (dateTimeStr?: string) => {
  if (!dateTimeStr) return '未知'
  
  const date = new Date(dateTimeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 格式化角色
const formatRoles = (roles?: any[]) => {
  // 调试输出，查看角色数据格式
  console.log('角色数据:', roles)
  
  if (!roles || roles.length === 0) return ['无角色']
  
  return roles.map(role => {
    if (typeof role === 'string') return role
    if (role && typeof role === 'object') {
      // 兼容多种可能的角色属性格式
      if (role.name) return role.name
      if (role.roleName) return role.roleName
      if (role.role) return role.role
    }
    return '未知角色'
  })
}

// 根据搜索条件筛选用户
const filteredUsers = computed(() => {
  if (!searchQuery.value) {
    return users.value
  }
  
  const query = searchQuery.value.toLowerCase()
  return users.value.filter(user => 
    user.username.toLowerCase().includes(query)
  )
})

// 初始化数据
const fetchData = async () => {
  loading.value = true
  try {
    // 获取用户列表
    const userList = await getAllUsers()
    users.value = userList || []
    
    // 获取所有角色
    const roles = await getAllRoles()
    allRoles.value = roles || []
  } catch (error) {
    console.error('获取数据失败:', error)
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 刷新用户列表
const refreshUserList = () => {
  fetchData()
}

// 处理搜索
const handleSearch = () => {
  // 直接使用计算属性进行过滤，无需额外处理
}

// 处理多选
const handleSelectionChange = (selection: User[]) => {
  selectedUsers.value = selection
}

// 处理编辑用户
const handleEdit = (user: User) => {
  editForm.value = {
    id: user.id,
    username: user.username
  }
  editDialogVisible.value = true
}

// 处理编辑角色
const handleEditRoles = (user: User) => {
  const userRoles = user.roles || []
  const selectedRoleIds = userRoles
    .filter((role: any) => typeof role === 'object' && role.id)
    .map((role: any) => role.id)
  
  roleForm.value = {
    userId: user.id,
    selectedRoles: selectedRoleIds
  }
  
  roleDialogVisible.value = true
}

// 保存用户信息
const handleSaveUser = async () => {
  try {
    await updateUser(editForm.value.id, {
      username: editForm.value.username
    })
    
    ElMessage.success('用户信息已更新')
    editDialogVisible.value = false
    refreshUserList()
  } catch (error) {
    console.error('更新用户失败:', error)
    ElMessage.error('更新用户失败')
  }
}

// 保存用户角色
const handleSaveRoles = async () => {
  try {
    await assignRolesToUser(
      roleForm.value.userId,
      roleForm.value.selectedRoles
    )
    
    ElMessage.success('用户角色已更新')
    roleDialogVisible.value = false
    refreshUserList()
  } catch (error) {
    console.error('更新用户角色失败:', error)
    ElMessage.error('更新用户角色失败')
  }
}

// 处理用户状态变更
const handleStatusChange = async (user: User, status: number) => {
  const statusText = status === 1 ? '启用' : '禁用'
  
  try {
    await ElMessageBox.confirm(
      `确定要${statusText}用户 "${user.username}" 吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await updateUserStatus(user.id, status)
    ElMessage.success(`用户已${statusText}`)
    refreshUserList()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error(`${statusText}用户失败:`, error)
      ElMessage.error(`${statusText}用户失败`)
    }
  }
}

// 批量修改用户状态
const handleBatchStatusChange = async (status: number) => {
  const statusText = status === 1 ? '启用' : '禁用'
  const userCount = selectedUsers.value.length
  
  try {
    await ElMessageBox.confirm(
      `确定要批量${statusText}已选择的 ${userCount} 个用户吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const userIds = selectedUsers.value.map(user => user.id)
    await batchUpdateUserStatus(userIds, status)
    ElMessage.success(`已成功${statusText} ${userCount} 个用户`)
    refreshUserList()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error(`批量${statusText}用户失败:`, error)
      ElMessage.error(`批量${statusText}用户失败`)
    }
  }
}

// 生命周期钩子
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.admin-container {
  padding: 20px;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.admin-tools {
  display: flex;
  gap: 10px;
  align-items: center;
}

.role-tag {
  margin-right: 5px;
}

:deep(.el-tag) {
  margin: 2px;
}
</style> 