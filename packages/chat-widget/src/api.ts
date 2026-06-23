/**
 * 聊天组件 API 调用封装
 */

export interface ChatRequest {
  question: string
}

export interface ChatResponse {
  answer: string
  sources: Array<{
    documentTitle: string
    content: string
  }>
}

/** 发送对话请求 */
export async function chat(baseUrl: string, request: ChatRequest): Promise<ChatResponse> {
  const response = await fetch(`${baseUrl}/api/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    throw new Error(`Chat API error: ${response.status}`)
  }

  const data = await response.json()
  return data.data
}
