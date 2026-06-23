/**
 * 聊天组件类型定义
 */

/** 组件初始化配置 */
export interface WidgetConfig {
  /** API Key（由租户管理员生成） */
  apiKey: string
  /** 后端 API 基础 URL */
  baseUrl?: string
  /** 品牌色（十六进制） */
  brandColor?: string
  /** 欢迎语 */
  welcomeText?: string
  /** 自定义图标 URL */
  iconUrl?: string
  /** 组件挂载目标（默认 body） */
  target?: string | HTMLElement
}
