import type { WidgetConfig, ChatRequest, ChatResponse } from './types'
import { chat } from './api'
import './styles/widget.css'

/**
 * 聊天组件主类
 *
 * 负责创建 UI、管理对话状态、调用后端 API
 */
export class ChatWidget {
  private config: WidgetConfig
  private container: HTMLElement | null = null

  constructor(config: WidgetConfig) {
    this.config = config
  }

  /** 挂载组件到页面 */
  mount(): void {
    this.container = document.createElement('div')
    this.container.className = 'docchat-widget'
    this.container.innerHTML = `
      <div class="docchat-widget__header">
        <span>${this.config.welcomeText || '有什么可以帮您？'}</span>
        <button class="docchat-widget__close">&times;</button>
      </div>
      <div class="docchat-widget__body">
        <div class="docchat-widget__messages"></div>
      </div>
      <div class="docchat-widget__input">
        <input type="text" placeholder="输入您的问题..." />
        <button>发送</button>
      </div>
    `

    document.body.appendChild(this.container)
    this.bindEvents()
  }

  /** 销毁组件 */
  destroy(): void {
    this.container?.remove()
    this.container = null
  }

  /** 发送消息 */
  async sendMessage(question: string): Promise<void> {
    const request: ChatRequest = { question }
    const response: ChatResponse = await chat(this.config.baseUrl!, request)
    this.appendMessage('user', question)
    this.appendMessage('bot', response.answer)
  }

  private bindEvents(): void {
    // TODO: 绑定关闭按钮、发送按钮、回车键事件
  }

  private appendMessage(role: 'user' | 'bot', content: string): void {
    // TODO: 渲染消息到 DOM
  }
}
