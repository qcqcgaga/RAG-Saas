import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 对话相关 API
 */

export interface ChatRequest {
  question: string
}

export interface ChatMessage {
  answer: string
  sources: Array<{
    documentTitle: string
    content: string
  }>
}

/** 发送对话 */
export function chat(data: ChatRequest) {
  return request.post<R<ChatMessage>>('/chat', data)
}
