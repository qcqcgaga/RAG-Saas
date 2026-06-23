/** 租户相关类型定义 */

export interface Tenant {
  id: number
  name: string
  slug: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface User {
  id: number
  email: string
  displayName: string
  tenantId: number
  role: 'admin' | 'member' | 'readonly'
  status: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  user: User
}

export interface RegisterRequest {
  tenantName: string
  slug: string
  email: string
  password: string
  displayName?: string
}
