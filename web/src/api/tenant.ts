import request from '@/utils/request'
import type { R } from '@/types/api'
import type { LoginRequest, LoginResponse, RegisterRequest } from '@/types/tenant'

/**
 * 租户相关 API
 */

/** 用户登录 */
export function login(data: LoginRequest) {
  return request.post<R<LoginResponse>>('/tenant/login', data)
}

/** 用户注册（创建租户） */
export function register(data: RegisterRequest) {
  return request.post<R<LoginResponse>>('/tenant/register', data)
}

/** 获取当前用户信息 */
export function getCurrentUser() {
  return request.get<R<LoginResponse>>('/tenant/me')
}
