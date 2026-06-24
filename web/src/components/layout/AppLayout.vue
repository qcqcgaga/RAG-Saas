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
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { BookOutlined, ClockCircleOutlined, CodeOutlined, UserOutlined, TeamOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { getCurrentTenant } from '@/api/tenant'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const collapsed = ref(false)
const tenantName = ref('')

const selectedKeys = computed(() => {
  const path = route.path.split('/')[1] || 'knowledge'
  return [path]
})

function handleLogout() {
  userStore.clearAuth()
  router.push({ name: 'login' })
}

onMounted(async () => {
  try {
    const res = await getCurrentTenant()
    tenantName.value = res.data.name
  } catch {
    // 忽略
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
