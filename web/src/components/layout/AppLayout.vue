<template>
  <a-layout class="app-layout">
    <a-layout-sider v-model:collapsed="collapsed" collapsible theme="light" :width="220">
      <div class="sider-logo">
        <h2 v-if="!collapsed">DocChat</h2>
        <h2 v-else>DC</h2>
      </div>
      <a-menu v-model:selectedKeys="selectedKeys" theme="light" mode="inline">
        <a-menu-item key="tenant" @click="router.push('/tenant')">
          <template #icon><TeamOutlined /></template>
          <span>租户管理</span>
        </a-menu-item>
        <a-menu-item key="knowledge" @click="router.push('/knowledge')">
          <template #icon><BookOutlined /></template>
          <span>知识库</span>
        </a-menu-item>
        <a-menu-item key="task" @click="router.push('/task')">
          <template #icon><ClockCircleOutlined /></template>
          <span>任务</span>
        </a-menu-item>
        <a-menu-item key="widget" @click="router.push('/widget')">
          <template #icon><CodeOutlined /></template>
          <span>聊天组件</span>
        </a-menu-item>
        <a-menu-divider />
        <a-menu-item key="apikey" @click="router.push('/apikey')">
          <template #icon><KeyOutlined /></template>
          <span>API Key 管理</span>
        </a-menu-item>
        <a-menu-item key="stats" @click="router.push('/stats')">
          <template #icon><BarChartOutlined /></template>
          <span>用量统计</span>
        </a-menu-item>
        <a-menu-item key="eval" @click="router.push('/eval')">
          <template #icon><ExperimentOutlined /></template>
          <span>评测集</span>
        </a-menu-item>
        <a-menu-item v-if="isAdminMenu" key="llm-config" @click="router.push('/llm-config')">
          <template #icon><RobotOutlined /></template>
          <span>LLM 配置</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="app-header">
        <div class="header-right">
          <a-dropdown>
            <a class="header-user" @click.prevent>
              <UserOutlined />
              <span style="margin-left: 8px">{{ tenantName }}</span>
            </a>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="handleLogout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>
      <a-layout-content class="app-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { BookOutlined, ClockCircleOutlined, CodeOutlined, UserOutlined, TeamOutlined, KeyOutlined, BarChartOutlined, ExperimentOutlined, RobotOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { getCurrentTenant } from '@/api/tenant'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const collapsed = ref(false)
const tenantName = ref('')

// 双重检查：userStore.isAdmin + localStorage role
// 防止切换账号后菜单权限显示异常
const isAdminMenu = computed(() => {
  const storeRole = userStore.role
  const lsRole = localStorage.getItem('role') || ''
  return storeRole === 'ADMIN' || lsRole === 'ADMIN'
})

const selectedKeys = computed(() => {
  const path = route.path.split('/')[1] || 'knowledge'
  return [path]
})

function handleLogout() {
  userStore.clearAuth()
  router.push({ name: 'login' })
}

onMounted(async () => {
  // 确保从 localStorage 恢复最新的 role 状态
  // 防止切换账号后菜单权限显示异常
  const storedRole = localStorage.getItem('role') || ''
  if (storedRole && storedRole !== userStore.role) {
    userStore.role = storedRole
  }

  try {
    const res = await getCurrentTenant()
    tenantName.value = res.data.name
  } catch {
    // 忽略
  }
})

// 监听 userStore.role 变化，同步到 localStorage
watch(() => userStore.role, (newRole) => {
  if (newRole) {
    localStorage.setItem('role', newRole)
  }
})
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
}

.sider-logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #f0f0f0;
}

.sider-logo h2 {
  color: #1890ff;
  margin: 0;
  font-size: 20px;
}

.app-header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.header-right {
  display: flex;
  align-items: center;
}

.header-user {
  color: rgba(0, 0, 0, 0.65);
  cursor: pointer;
}

.app-content {
  margin: 24px;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  min-height: 360px;
}
</style>
