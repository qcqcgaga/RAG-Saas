<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">DocChat</h1>
      <p class="login-subtitle">文档智能客服 SaaS</p>

      <a-tabs v-model:activeKey="activeTab" centered>
        <a-tab-pane key="login" tab="登录">
          <a-form :model="loginForm" @finish="handleLogin" layout="vertical">
            <a-form-item name="email" :rules="[{ required: true, message: '请输入邮箱' }, { type: 'email', message: '邮箱格式不正确' }]">
              <a-input v-model:value="loginForm.email" placeholder="邮箱" size="large" />
            </a-form-item>
            <a-form-item name="password" :rules="[{ required: true, message: '请输入密码' }]">
              <a-input-password v-model:value="loginForm.password" placeholder="密码" size="large" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" :loading="loginLoading" block size="large">
                登录
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="register" tab="注册">
          <a-form :model="registerForm" @finish="handleRegister" layout="vertical">
            <a-form-item name="email" :rules="[{ required: true, message: '请输入邮箱' }, { type: 'email', message: '邮箱格式不正确' }]">
              <a-input v-model:value="registerForm.email" placeholder="邮箱" size="large" />
            </a-form-item>
            <a-form-item name="password" :rules="[{ required: true, message: '请输入密码' }, { min: 8, message: '密码至少8位' }]">
              <a-input-password v-model:value="registerForm.password" placeholder="密码（至少8位，含字母和数字）" size="large" />
            </a-form-item>
            <a-form-item name="tenantName" :rules="[{ required: true, message: '请输入团队名称' }]">
              <a-input v-model:value="registerForm.tenantName" placeholder="团队名称" size="large" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit" :loading="registerLoading" block size="large">
                注册
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { login, register } from '@/api/tenant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loginLoading = ref(false)
const registerLoading = ref(false)

const loginForm = reactive({ email: '', password: '' })
const registerForm = reactive({ email: '', password: '', tenantName: '' })

async function handleLogin() {
  loginLoading.value = true
  try {
    const res = await login(loginForm)
    userStore.setAuth(res.data)
    message.success('登录成功')
    router.push('/')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loginLoading.value = false
  }
}

async function handleRegister() {
  registerLoading.value = true
  try {
    const res = await register(registerForm)
    userStore.setAuth(res.data)
    message.success('注册成功')
    router.push('/')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f0f2f5;
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.login-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  color: #1890ff;
  margin-bottom: 4px;
}

.login-subtitle {
  text-align: center;
  color: #999;
  margin-bottom: 24px;
}
</style>
