import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/tenant/LoginView.vue'),
    },
    {
      path: '/',
      component: () => import('@/components/layout/AppLayout.vue'),
      children: [
        {
          path: '',
          redirect: '/knowledge',
        },
        {
          path: 'tenant',
          name: 'tenant',
          component: () => import('@/views/tenant/TenantView.vue'),
        },
        {
          path: 'knowledge',
          name: 'knowledge',
          component: () => import('@/views/knowledge/KnowledgeView.vue'),
        },
        {
          path: 'task',
          name: 'task',
          component: () => import('@/views/task/TaskView.vue'),
        },
        {
          path: 'widget',
          name: 'widget',
          component: () => import('@/views/widget/WidgetView.vue'),
        },
        {
          path: 'apikey',
          name: 'apikey',
          component: () => import('@/views/apikey/index.vue'),
          meta: { title: 'API Key 管理' },
        },
        {
          path: 'stats',
          name: 'stats',
          component: () => import('@/views/stats/index.vue'),
          meta: { title: '用量统计' },
        },
        {
          path: 'eval',
          name: 'eval',
          component: () => import('@/views/eval/index.vue'),
          meta: { title: '评测集' },
        },
        {
          path: 'llm-config',
          name: 'llm-config',
          component: () => import('@/views/llm-config/index.vue'),
          meta: { title: 'LLM 配置', requiresAdmin: true },
        },
      ],
    },
  ],
})

// 路由守卫 — 未登录跳转 + 管理员页面权限
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!token && to.name !== 'login') {
    return { name: 'login' }
  }
  // 管理员专属页面权限检查
  if (to.meta?.requiresAdmin) {
    const role = localStorage.getItem('role') || ''
    if (role !== 'ADMIN') {
      return { name: 'knowledge' }
    }
  }
})

export default router
