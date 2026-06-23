<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible>
      <div class="logo">DocChat</div>
      <a-menu theme="dark" mode="inline" :selected-keys="selectedKeys">
        <a-menu-item key="knowledge" @click="$router.push('/knowledge')">
          <span>知识库</span>
        </a-menu-item>
        <a-menu-item key="task" @click="$router.push('/task')">
          <span>任务</span>
        </a-menu-item>
        <a-menu-item key="widget" @click="$router.push('/widget')">
          <span>聊天组件</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header style="background: #fff; padding: 0 16px">
        <span>DocChat 文档智能客服</span>
      </a-layout-header>
      <a-layout-content style="margin: 16px">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'

const collapsed = ref(false)
const route = useRoute()

const selectedKeys = computed(() => {
  const path = route.path.split('/')[1]
  return path ? [path] : ['knowledge']
})
</script>

<style scoped>
.logo {
  height: 32px;
  margin: 16px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  text-align: center;
  line-height: 32px;
}
</style>
