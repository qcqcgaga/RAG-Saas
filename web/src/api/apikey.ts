import request from '@/utils/request'

/**
 * API Key 管理
 */

/** API Key 响应字段（与后端 ApiKeyResponse 对齐） */
export interface ApiKeyData {
  id: number
  name: string
  key?: string           // 完整 Key，仅在创建时返回
  keyPrefix: string
  keyMasked: string
  status: number         // 1-有效 0-已吊销
  dailyLimit: number     // 每日限额
  lastUsedAt: string | null
  createdAt: string
}

/** 创建 API Key */
export function createApiKey(data: { name?: string }) {
  return request.post<any, { data: ApiKeyData }>('/api/v1/api-keys', data)
}

/** 获取 API Key 列表 */
export function listApiKeys() {
  return request.get<any, { data: ApiKeyData[] }>('/api/v1/api-keys')
}

/** 吊销 API Key */
export function revokeApiKey(keyId: number) {
  return request.delete<any, any>(`/api/v1/api-keys/${keyId}`, { data: { confirm: true } })
}

/** 重命名 API Key */
export function renameApiKey(keyId: number, name: string) {
  return request.put<any, any>(`/api/v1/api-keys/${keyId}/name`, { name })
}
