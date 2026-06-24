# 嵌入式聊天组件 (chat-widget)

> 输出 IIFE 格式 JS，可嵌入任意网页，类似 Intercom 轻量聊天 widget

## 目录结构

```
packages/chat-widget/
├── src/
│   ├── main.ts          # 组件入口(DOM创建+初始化)
│   ├── ChatWidget.ts    # 主组件类(聊天UI+消息管理+SSE对话)
│   ├── api.ts           # 后端API调用(SSE对话+获取配置)
│   ├── types.ts         # 类型定义(WidgetConfig+Message+ChatEvent)
│   └── styles/widget.css # 隔离样式(CSS Modules)
├── vite.config.ts       # Vite lib mode, 输出 IIFE
├── package.json
└── tsconfig.json
```

## 核心类: ChatWidget

```typescript
class ChatWidget {
  constructor(containerId: string, config: WidgetConfig)
  open(): void     // 打开聊天窗口
  close(): void    // 关闭聊天窗口
  sendMessage(text: string): void  // 发送消息(SSE)
  destroy(): void  // 清理DOM和事件
}
```

## 嵌入方式

```html
<script src="/widget.js" data-token="{widget_token}"></script>
```

组件自动创建：
- 悬浮按钮（右下角）
- 聊天窗口（标题栏+消息列表+输入框）
- 欢迎语

## 配置项 (WidgetConfig)

| 字段 | 类型 | 说明 |
|------|------|------|
| brandColor | string | 品牌色(默认#1890ff) |
| welcomeMessage | string | 欢迎语 |
| iconUrl | string | 自定义图标URL |
| enabled | boolean | 是否启用 |

## SSE 对话流程

```
用户输入 → api.ts: sendChat(question)
        → POST /api/v1/chat/conversations (Authorization: Bearer {widget_token})
        → EventSource 消费 SSE 流
        → token事件 → 追加消息内容
        → done事件 → 显示来源引用
        → error事件 → 显示错误提示
```

## 样式隔离

使用原生 CSS + Shadow DOM 或 CSS Modules 避免与宿主页面样式冲突。

## 详细文档

- [pitfalls.md](pitfalls.md)
