import request from '@/utils/request'
import type { AuthData, TenantInfo, MemberInfo, PageResult } from '@/types/api'

/** 用户注册 */
export function register(data: { email: string; password: string; tenantName: string }) {
  return request.post<any, { data: AuthData }>('/api/v1/auth/register', data)
}

/** 用户登录 */
export function login(data: { email: string; password: string }) {
  return request.post<any, { data: AuthData }>('/api/v1/auth/login', data)
}

/** 获取当前租户信息 */
export function getCurrentTenant() {
  return request.get<any, { data: TenantInfo }>('/api/v1/tenants/current')
}

/** 更新租户信息 */
export function updateCurrentTenant(data: { name: string }) {
  return request.put<any, { data: TenantInfo }>('/api/v1/tenants/current', data)
}

/** 获取成员列表 */
export function listMembers(page = 1, size = 20) {
  return request.get<any, { data: PageResult<MemberInfo> }>('/api/v1/tenants/members', { params: { page, size } })
}

/** 邀请成员 */
export function inviteMember(data: { email: string; role: string; password: string }) {
  return request.post<any, { data: MemberInfo }>('/api/v1/tenants/members', data)
}

/** 修改成员角色 */
export function updateMemberRole(userId: number, role: string) {
  return request.put<any, any>(`/api/v1/tenants/members/${userId}/role`, { role })
}

/** 移除成员 */
export function removeMember(userId: number) {
  return request.delete<any, any>(`/api/v1/tenants/members/${userId}`)
}
