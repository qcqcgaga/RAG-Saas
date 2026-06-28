import request from '@/utils/request'

/**
 * LLM 配置管理
 */

/** LLM 配置信息 */
export interface LlmConfig {
  apiUrl: string
  apiKeyMasked: string
  modelName: string
  status: number
  updatedAt: string | null
}

/** 连通性测试结果 */
export interface LlmTestResult {
  connected: boolean
  modelName: string
  responseTimeMs: number
}

/** 获取 LLM 配置 */
export function getLlmConfig() {
  return request.get<any, { data: LlmConfig }>('/api/v1/llm-config')
}

/** 更新 LLM 配置 */
export function updateLlmConfig(data: { apiUrl: string; apiKey: string; modelName: string }) {
  return request.put<any, { data: LlmConfig }>('/api/v1/llm-config', data)
}

/** 测试连通性 */
export function testLlmConfig(data: { apiUrl: string; apiKey: string; modelName: string }) {
  return request.post<any, { data: LlmTestResult }>('/api/v1/llm-config/test', data)
}

/** 恢复默认配置 */
export function deleteLlmConfig() {
  return request.delete<any, any>('/api/v1/llm-config')
}
