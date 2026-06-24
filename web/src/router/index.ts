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
      ],
    },
  ],
})

// 路由守卫 — 未登录跳转
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!token && to.name !== 'login') {
    return { name: 'login' }
  }
})

export default router
