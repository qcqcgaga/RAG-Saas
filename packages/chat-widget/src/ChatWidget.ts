import type { ChatWidgetOptions, WidgetConfig, SourceItem } from './types'
import { fetchConfig, streamChat } from './api'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export class ChatWidget {
  private apiKey: string      // 用于对话鉴权（dc_ 前缀的 API Key）
  private widgetToken: string // 用于获取组件配置（widget_token）
  private apiUrl: string
  private config: WidgetConfig | null = null
  private isOpen = false
  private isSending = false
  private container: HTMLElement | null = null
  private messagesContainer: HTMLElement | null = null
  private inputEl: HTMLInputElement | null = null
  private messages: ChatMessage[] = []

  constructor(options: ChatWidgetOptions) {
    // V1: apiKey 用于对话鉴权，token 用于获取配置
    this.apiKey = options.apiKey || options.token || ''
    this.widgetToken = options.token || options.apiKey || ''
    this.apiUrl = options.apiUrl || ''
    this.init()
  }

  private async init(): Promise<void> {
    console.log('[DocChat] 开始初始化, apiKey:', this.apiKey ? '***' : '(空)', 'widgetToken:', this.widgetToken ? '***' : '(空)', 'apiUrl:', this.apiUrl)
    await this.loadConfig()
    if (!this.config || !this.config.enabled) {
      console.warn('[DocChat] 配置加载失败或组件未启用, config:', this.config)
      return
    }
    console.log('[DocChat] 配置加载成功, enabled:', this.config.enabled)

    this.createDOM()
    this.bindPostMessage()
    this.postMessage('widget:ready', {})
  }

  private async loadConfig(): Promise<void> {
    // 使用 widgetToken 获取组件配置
    try {
      const data = await fetchConfig(this.apiUrl, this.widgetToken)
      if (data && data.code === 0) {
        this.config = data.data
      } else {
        console.warn('[DocChat] 配置接口返回异常:', data)
      }
    } catch (e) {
      console.error('[DocChat] 加载配置异常:', e)
    }
  }

  private createDOM(): void {
    this.container = document.createElement('div')
    this.container.className = 'docchat-root'
    document.body.appendChild(this.container)

    this.createTrigger()
    this.createChatWindow()
  }

  private createTrigger(): void {
    const trigger = document.createElement('div')
    trigger.className = 'docchat-trigger'
    trigger.style.backgroundColor = this.config!.brandColor

    if (this.config!.iconUrl) {
      trigger.innerHTML = `<img src="${this.config!.iconUrl}" alt="chat" class="docchat-trigger-icon" />`
    } else {
      trigger.innerHTML =
        '<svg viewBox="0 0 24 24" width="28" height="28" fill="white"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/></svg>'
    }

    trigger.addEventListener('click', () => this.toggle())
    this.container!.appendChild(trigger)
  }

  private createChatWindow(): void {
    const chatWindow = document.createElement('div')
    chatWindow.className = 'docchat-window'

    // 头部
    const header = document.createElement('div')
    header.className = 'docchat-header'
    header.style.backgroundColor = this.config!.brandColor
    header.innerHTML = `
      <span class="docchat-header-title">在线客服</span>
      <button class="docchat-close-btn" aria-label="关闭">&times;</button>
    `
    header.querySelector('.docchat-close-btn')!.addEventListener('click', () => this.toggle())
    chatWindow.appendChild(header)

    // 消息区域
    this.messagesContainer = document.createElement('div')
    this.messagesContainer.className = 'docchat-messages'

    // Only add welcome message on initial creation, not during re-render
    if (this.messages.length === 0 && this.config!.welcomeMessage) {
      this.messages.push({ role: 'assistant', content: this.config!.welcomeMessage })
      this.addBotMessage(this.config!.welcomeMessage)
    }

    chatWindow.appendChild(this.messagesContainer)

    // 输入区域
    const inputArea = document.createElement('div')
    inputArea.className = 'docchat-input-area'

    this.inputEl = document.createElement('input')
    this.inputEl.type = 'text'
    this.inputEl.className = 'docchat-input'
    this.inputEl.placeholder = '输入您的问题...'
    this.inputEl.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !this.isSending) this.sendMessage()
    })
    inputArea.appendChild(this.inputEl)

    const sendBtn = document.createElement('button')
    sendBtn.className = 'docchat-send-btn'
    sendBtn.style.backgroundColor = this.config!.brandColor
    sendBtn.textContent = '发送'
    sendBtn.addEventListener('click', () => {
      if (!this.isSending) this.sendMessage()
    })
    inputArea.appendChild(sendBtn)

    chatWindow.appendChild(inputArea)
    this.container!.appendChild(chatWindow)
  }

  private bindPostMessage(): void {
    window.addEventListener('message', (event: MessageEvent) => {
      // Security: validate origin
      const allowedOrigins = this.config?.allowedOrigins || []
      if (allowedOrigins.length > 0 && !allowedOrigins.includes(event.origin)) {
        return
      }

      const data = event.data
      if (data.type === 'config-update') {
        // Hot update appearance config
        this.updateConfig(data.config)
      } else if (data.type === 'reset') {
        // Reset conversation
        this.reset()
      }
    })
  }

  /** Reset conversation and re-render */
  public reset(): void {
    this.messages = []
    // Re-add welcome message
    if (this.config?.welcomeMessage) {
      this.messages.push({ role: 'assistant', content: this.config.welcomeMessage })
    }
    this.render()
  }

  /** Hot-update appearance config and re-render */
  public updateConfig(config: Partial<WidgetConfig>): void {
    if (config.brandColor) {
      this.config!.brandColor = config.brandColor
    }
    if (config.welcomeMessage) {
      this.config!.welcomeMessage = config.welcomeMessage
    }
    if (config.iconUrl) {
      this.config!.iconUrl = config.iconUrl
    }
    this.render()
  }

  /** Re-render the entire widget DOM from current state */
  private render(): void {
    if (!this.container) return

    // Remove existing DOM children
    this.container.innerHTML = ''
    this.messagesContainer = null
    this.inputEl = null

    this.createTrigger()
    this.createChatWindow()

    // Restore messages from state
    for (const msg of this.messages) {
      if (msg.role === 'user') {
        this.addUserMessage(msg.content)
      } else {
        this.addBotMessage(msg.content)
      }
    }
  }

  private toggle(): void {
    this.isOpen = !this.isOpen
    const chatWindow = this.container!.querySelector('.docchat-window') as HTMLElement
    const trigger = this.container!.querySelector('.docchat-trigger') as HTMLElement

    if (this.isOpen) {
      chatWindow.classList.add('docchat-window--open')
      trigger.classList.add('docchat-trigger--hidden')
      if (this.inputEl) this.inputEl.focus()
    } else {
      chatWindow.classList.remove('docchat-window--open')
      trigger.classList.remove('docchat-trigger--hidden')
    }
  }

  private async sendMessage(): Promise<void> {
    if (!this.inputEl || !this.inputEl.value.trim()) return
    const question = this.inputEl.value.trim()
    this.inputEl.value = ''
    this.isSending = true

    this.messages.push({ role: 'user', content: question })
    this.addUserMessage(question)
    const botMsgEl = this.addBotMessage('', true)

    await streamChat(this.apiUrl, this.apiKey, question, {
      onToken: (text: string) => {
        this.updateBotMessage(botMsgEl, text)
      },
      onDone: (sources: SourceItem[]) => {
        this.removeLoading(botMsgEl)
        if (sources.length > 0) {
          this.addSources(botMsgEl, sources)
        }
        // Store final assistant message
        this.messages.push({ role: 'assistant', content: botMsgEl.textContent || '' })
        this.isSending = false
      },
      onError: (message: string) => {
        this.updateBotMessage(botMsgEl, message)
        this.removeLoading(botMsgEl)
        this.messages.push({ role: 'assistant', content: message })
        this.isSending = false
      },
    })
  }

  private addUserMessage(text: string): void {
    const msg = document.createElement('div')
    msg.className = 'docchat-message docchat-user-message'
    msg.textContent = text
    this.messagesContainer!.appendChild(msg)
    this.scrollToBottom()
  }

  private addBotMessage(text: string, loading = false): HTMLElement {
    const msg = document.createElement('div')
    msg.className = 'docchat-message docchat-bot-message'
    if (loading) {
      msg.innerHTML = '<span class="docchat-loading"><span class="docchat-loading-dot"></span><span class="docchat-loading-dot"></span><span class="docchat-loading-dot"></span></span>'
    } else {
      msg.textContent = text
    }
    this.messagesContainer!.appendChild(msg)
    this.scrollToBottom()
    return msg
  }

  private updateBotMessage(el: HTMLElement, text: string): void {
    this.removeLoading(el)
    el.textContent = text
    this.scrollToBottom()
  }

  private removeLoading(el: HTMLElement): void {
    const loadingEl = el.querySelector('.docchat-loading')
    if (loadingEl) loadingEl.remove()
  }

  private addSources(el: HTMLElement, sources: SourceItem[]): void {
    const sourcesEl = document.createElement('div')
    sourcesEl.className = 'docchat-sources'
    sourcesEl.innerHTML = '<div class="docchat-sources-title">来源引用</div>'
    for (const s of sources) {
      const sourceEl = document.createElement('div')
      sourceEl.className = 'docchat-source-item'
      sourceEl.textContent = `${s.documentName} (第${s.chunkIndex}段)`
      sourcesEl.appendChild(sourceEl)
    }
    el.appendChild(sourcesEl)
    this.scrollToBottom()
  }

  private scrollToBottom(): void {
    if (this.messagesContainer) {
      this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight
    }
  }

  private postMessage(type: string, data: Record<string, unknown>): void {
    window.parent.postMessage({ type, source: 'docchat-widget', ...data }, '*')
  }
}
