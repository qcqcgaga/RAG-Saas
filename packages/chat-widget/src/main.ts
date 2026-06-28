import { ChatWidget } from './ChatWidget'
import './styles/widget.css'

// 从 script 标签的 data-* 属性或全局变量获取配置
// 优先级：window.__DOCCHAT_CONFIG__ > document.currentScript dataset
// data-api-key: 用于对话鉴权（V1 推荐）
// data-token: 用于获取组件配置（widget_token）
// data-api-url: 后端 API 地址

declare global {
  interface Window {
    __DOCCHAT_CONFIG__?: {
      apiKey?: string
      token?: string
      apiUrl?: string
    }
  }
}

// 优先从全局变量读取（widget-preview.html 使用），再从 script dataset 读取
const globalConfig = window.__DOCCHAT_CONFIG__
const scriptEl = document.currentScript as HTMLScriptElement | null

const apiKey = globalConfig?.apiKey || scriptEl?.dataset.apiKey || ''
const token = globalConfig?.token || scriptEl?.dataset.token || apiKey  // data-token 优先，默认用 apiKey
const apiUrl = globalConfig?.apiUrl || scriptEl?.dataset.apiUrl || ''

// 调试日志：输出组件初始化参数
console.log('[DocChat] 初始化参数:', {
  source: globalConfig ? 'window.__DOCCHAT_CONFIG__' : 'script.dataset',
  apiKey: apiKey ? '***' : '(空)',
  token: token ? '***' : '(空)',
  apiUrl: apiUrl || '(空)'
})

// 初始化 Widget
new ChatWidget({
  apiKey: apiKey || undefined,
  token: token || undefined,
  apiUrl: apiUrl || undefined,
})
