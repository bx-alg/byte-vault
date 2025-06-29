<template>
  <div class="login-container">
    <div class="login-card">
      <div class="card-decoration">
        <img src="@/assets/cute.jpeg" class="decoration-image floating" alt="装饰" />
      </div>
      
      <h2 class="login-title">
        <span class="title-text">ByteVault</span>
        <span class="subtitle">文件管理系统</span>
      </h2>
      
      <div class="form-container">
        <el-tabs v-model="activeTab" class="login-tabs">
          <el-tab-pane label="登录" name="login">
            <el-form :model="loginForm" ref="loginFormRef" :rules="loginRules" class="login-form">
              <el-form-item prop="username">
                <el-input 
                  v-model="loginForm.username"
                  placeholder="用户名"
                  prefix-icon="el-icon-user"
                >
                  <template #prefix>
                    <el-icon><User /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              
              <el-form-item prop="password">
                <el-input 
                  v-model="loginForm.password"
                  type="password"
                  placeholder="密码"
                  show-password
                >
                  <template #prefix>
                    <el-icon><Lock /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              
              <el-form-item>
                <el-button 
                  type="primary" 
                  :loading="loading"
                  @click="handleLogin"
                  class="submit-btn wiggle"
                >
                  登录
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
          
          <el-tab-pane label="注册" name="register">
            <el-form :model="registerForm" ref="registerFormRef" :rules="registerRules" class="login-form">
              <el-form-item prop="username">
                <el-input 
                  v-model="registerForm.username"
                  placeholder="用户名"
                >
                  <template #prefix>
                    <el-icon><User /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              
              <el-form-item prop="password">
                <el-input 
                  v-model="registerForm.password"
                  type="password"
                  placeholder="密码"
                  show-password
                >
                  <template #prefix>
                    <el-icon><Lock /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              
              <el-form-item prop="confirmPassword">
                <el-input 
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="确认密码"
                  show-password
                >
                  <template #prefix>
                    <el-icon><Key /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              
              <el-form-item>
                <el-button 
                  type="primary" 
                  :loading="loading"
                  @click="handleRegister"
                  class="submit-btn wiggle"
                >
                  注册
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
      
      <div class="login-footer">
        <p>欢迎使用ByteVault文件管理系统</p>
        <p class="copyright">© 2024 ByteVault</p>
      </div>
    </div>
    
    <div class="bottom-decoration">
      <img src="@/assets/_.jpeg" class="bottom-image" alt="页脚装饰" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const activeTab = ref('login')

// 登录表单
const loginFormRef = ref<FormInstance>()
const loginForm = reactive({
  username: '',
  password: ''
})

// 注册表单
const registerFormRef = ref<FormInstance>()
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

// 表单验证规则
const loginRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 5, max: 20, message: '密码长度应为5-20个字符', trigger: 'blur' }
  ]
})

const registerRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度应为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 5, max: 20, message: '密码长度应为5-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
})

// 登录处理
const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const result = await userStore.loginAction(loginForm.username, loginForm.password)
        if (result) {
          ElMessage.success('登录成功')
          router.push('/')
        } else {
          ElMessage.error('登录失败，请检查用户名和密码')
        }
      } catch (error) {
        console.error('登录失败:', error)
        ElMessage.error('登录失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
  })
}

// 注册处理
const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const result = await userStore.registerAction(
          registerForm.username, 
          registerForm.password,
          registerForm.confirmPassword
        )
        
        if (result) {
          ElMessage.success('注册成功，请登录')
          activeTab.value = 'login'
          loginForm.username = registerForm.username
          loginForm.password = ''
          
          // 清空注册表单
          registerForm.username = ''
          registerForm.password = ''
          registerForm.confirmPassword = ''
        } else {
          ElMessage.error('注册失败，用户名可能已存在')
        }
      } catch (error) {
        console.error('注册失败:', error)
        ElMessage.error('注册失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  position: relative;
}

.login-card {
  width: 100%;
  max-width: 450px;
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
  padding: 30px;
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 105, 180, 0.2);
  z-index: 1;
}

.card-decoration {
  position: absolute;
  top: -20px;
  right: -20px;
  z-index: -1;
  opacity: 0.8;
}

.decoration-image {
  width: 150px;
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
  position: relative;
}

.title-text {
  font-size: 2.5rem;
  font-weight: 800;
  color: var(--primary-color);
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
  display: block;
}

.subtitle {
  font-size: 1.2rem;
  color: var(--secondary-color);
  font-weight: normal;
}

.form-container {
  margin-bottom: 30px;
}

.login-tabs :deep(.el-tabs__item) {
  font-size: 1.1rem;
  padding: 0 20px;
  color: var(--text-color);
}

.login-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--primary-color);
  height: 3px;
  border-radius: 3px;
}

.login-tabs :deep(.el-tabs__item.is-active) {
  color: var(--primary-color);
  font-weight: 600;
}

.login-form {
  margin-top: 20px;
}

.login-form :deep(.el-input__inner) {
  height: 50px;
  font-size: 16px;
}

.login-form :deep(.el-input__prefix) {
  display: flex;
  align-items: center;
  color: var(--primary-color);
  font-size: 18px;
}

.submit-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  letter-spacing: 2px;
  margin-top: 10px;
}

.login-footer {
  text-align: center;
  color: var(--light-text);
  font-size: 0.9rem;
}

.copyright {
  margin-top: 5px;
  font-size: 0.8rem;
}

.bottom-decoration {
  position: fixed;
  bottom: 0;
  right: 0;
  z-index: 0;
}

.bottom-image {
  width: 200px;
  opacity: 0.7;
}
</style> 