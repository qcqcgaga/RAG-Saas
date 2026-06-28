/**
 * 聊天组件 API 调用封装
 */

import type { ConfigResponse, SseEvent, SourceItem } from './types'

/** 加载组件远程配置 */
export async function fetchConfig(
  apiUrl: string,
  credential: string,
): Promise<ConfigResponse | null> {
  try {
    const url = `${apiUrl}/api/v1/widget/config?token=${encodeURIComponent(credential)}`
    console.log('[DocChat] 请求配置:', url.replace(credential, '***'))
    const res = await fetch(url)
    if (!res.ok) {
      console.error('[DocChat] 配置接口HTTP错误:', res.status, res.statusText)
      return null
    }
    const data: ConfigResponse = await res.json()
    console.log('[DocChat] 配置接口响应 code:', data.code)
    return data
  } catch (e) {
    console.error('[DocChat] 加载配置失败:', e)
    return null
  }
}

/** SSE 流式对话回调 */
export interface ChatCallbacks {
  onToken: (text: string) => void
  onDone: (sources: SourceItem[]) => void
  onError: (message: string) => void
}

/** 发送对话请求（SSE 流式） */
export async function streamChat(
  apiUrl: string,
  credential: string,
  question: string,
  callbacks: ChatCallbacks,
): Promise<void> {
  let fullText = ''

  try {
    const response = await fetch(`${apiUrl}/api/v1/chat/conversations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${credential}`,
      },
      body: JSON.stringify({ question }),
    })

    if (!response.ok || !response.body) {
      throw new Error(`请求失败: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      // 按 \n\n 分割 SSE 事件块
      const parts = buffer.split('\n\n')
      buffer = parts.pop() || ''

      for (const part of parts) {
        const event = parseSseEvent(part)
        if (!event) continue

        if (event.type === 'token') {
          fullText += event.content
          callbacks.onToken(fullText)
        } else if (event.type === 'done') {
          callbacks.onDone(event.sources || [])
        } else if (event.type === 'error') {
          callbacks.onError(event.content)
        }
      }
    }

    // 处理可能残留的 buffer
    if (buffer.trim()) {
      const event = parseSseEvent(buffer)
      if (event) {
        if (event.type === 'token') {
          fullText += event.content
          callbacks.onToken(fullText)
        } else if (event.type === 'done') {
          callbacks.onDone(event.sources || [])
        } else if (event.type === 'error') {
          callbacks.onError(event.content)
        }
      }
    }
  } catch (e) {
    callbacks.onError('抱歉，服务暂时不可用，请稍后重试。')
  }
}

/** 解析单个 SSE 事件块 */
function parseSseEvent(chunk: string): SseEvent | null {
  const lines = chunk.split('\n')
  for (const line of lines) {
    if (line.startsWith('data:')) {
      try {
        return JSON.parse(line.substring(5).trim()) as SseEvent
      } catch {
        // 忽略解析错误
      }
    }
  }
  return null
}
