/**
 * 聊天组件类型定义
 */

/** 组件远程配置（从后端 /api/v1/widget/config 获取） */
export interface WidgetConfig {
  brandColor: string
  welcomeMessage: string
  iconUrl: string | null
  enabled: boolean
  allowedOrigins?: string[]
}

/** 组件初始化选项（从 script 标签 data-* 属性获取） */
export interface ChatWidgetOptions {
  /** V1: API Key 鉴权（推荐） */
  apiKey?: string
  /** 旧版 widget_token（过渡期兼容） */
  token?: string
  apiUrl?: string
}

/** SSE token 事件 */
export interface SseTokenEvent {
  type: 'token'
  content: string
}

/** SSE done 事件 */
export interface SseDoneEvent {
  type: 'done'
  content: ''
  sources: SourceItem[]
}

/** SSE error 事件 */
export interface SseErrorEvent {
  type: 'error'
  content: string
}

export type SseEvent = SseTokenEvent | SseDoneEvent | SseErrorEvent

/** 来源引用条目 */
export interface SourceItem {
  documentName: string
  chunkIndex: number
}

/** 后端配置接口响应 */
export interface ConfigResponse {
  code: number
  data: WidgetConfig
}
