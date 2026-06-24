import request from '@/utils/request'

/**
 * 对话相关 API
 */

export interface ChatRequest {
  question: string
}

export interface ChatSource {
  documentTitle: string
  content: string
}

export interface ChatMessage {
  answer: string
  sources: ChatSource[]
}

/** 发送对话 */
export function chat(data: ChatRequest) {
  return request.post<any, { data: ChatMessage }>('/api/v1/chat', data)
}
