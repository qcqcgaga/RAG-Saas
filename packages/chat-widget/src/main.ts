import { ChatWidget } from './ChatWidget'
import type { WidgetConfig } from './types'

/**
 * 聊天组件入口
 *
 * 全局暴露 DocChatWidget 对象，供嵌入页面调用：
 * DocChatWidget.init({ apiKey: 'xxx', baseUrl: 'https://api.docchat.com' })
 */
function init(config: WidgetConfig): ChatWidget {
  const widget = new ChatWidget(config)
  widget.mount()
  return widget
}

// 暴露到全局
const DocChatWidget = { init }

// 兼容直接 script 标签引入
if (typeof window !== 'undefined') {
  (window as unknown as Record<string, unknown>).DocChatWidget = DocChatWidget
}

export { init }
export default DocChatWidget
