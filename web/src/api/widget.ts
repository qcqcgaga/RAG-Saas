import request from '@/utils/request'
import type { WidgetConfig } from '@/types/api'

/**
 * 聊天组件相关 API
 */

/** 获取组件配置 */
export function getWidgetConfig(token: string) {
  return request.get<any, { data: WidgetConfig }>('/api/v1/widget/config', { params: { token } })
}

/** 更新组件配置 */
export function updateWidgetConfig(data: Partial<WidgetConfig>) {
  return request.put<any, { data: WidgetConfig }>('/api/v1/widget/config', data)
}

/** 获取嵌入脚本 */
export function getEmbedScript() {
  return request.get<any, { data: { script: string; previewUrl: string } }>('/api/v1/widget/embed-script')
}

/** 重新生成 Token */
export function regenerateToken() {
  return request.post<any, { data: { token: string } }>('/api/v1/widget/regenerate-token')
}
