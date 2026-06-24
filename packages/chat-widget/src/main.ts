import { ChatWidget } from './ChatWidget'
import './styles/widget.css'

// 从 script 标签的 data-token 属性获取配置
const scripts = document.getElementsByTagName('script')
const currentScript = scripts[scripts.length - 1]
const token = currentScript?.getAttribute('data-token') || ''
const apiUrl = currentScript?.getAttribute('data-api-url') || ''

// 初始化 Widget
new ChatWidget({
  token,
  apiUrl: apiUrl || undefined,
})
