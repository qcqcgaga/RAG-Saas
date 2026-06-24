import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AuthData } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userId = ref<number | null>(null)
  const tenantId = ref<number | null>(null)
  const role = ref<string>('')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  function setAuth(data: AuthData) {
    token.value = data.token
    userId.value = data.userId
    tenantId.value = data.tenantId
    role.value = data.role
    localStorage.setItem('token', data.token)
  }

  function clearAuth() {
    token.value = ''
    userId.value = null
    tenantId.value = null
    role.value = ''
    localStorage.removeItem('token')
  }

  return { token, userId, tenantId, role, isLoggedIn, isAdmin, setAuth, clearAuth }
})
