# 管理后台 (web-admin)

> Vue 3 + Ant Design Vue + Pinia + TypeScript 管理后台

## 目录结构

```
web/src/
├── main.ts              # 应用入口
├── App.vue              # 根组件
├── api/                 # 后端 API 调用封装
│   ├── tenant.ts        # 认证 + 租户管理
│   ├── knowledge.ts     # 知识库 + 文档管理
│   ├── task.ts          # 异步任务
│   ├── chat.ts          # 对话(调试用)
│   └── widget.ts        # 组件配置
├── views/               # 页面组件
│   ├── tenant/          # 登录 + 租户管理
│   ├── knowledge/       # 知识库管理(含版本面板)
│   ├── task/            # 任务状态
│   └── widget/          # 组件配置
├── stores/              # Pinia 状态管理
│   ├── user.ts          # 用户认证状态
│   ├── tenant.ts        # 租户信息
│   └── knowledge.ts     # 知识库状态
├── components/layout/   # 布局组件
│   └── AppLayout.vue    # 主布局(Sidebar+Header+Content)
├── router/index.ts      # 路由配置(含导航守卫)
├── utils/request.ts     # Axios 封装(JWT拦截器+错误处理)
├── types/               # TypeScript 类型定义
│   ├── api.d.ts         # 通用响应类型
│   ├── tenant.d.ts      # 租户相关类型
│   └── knowledge.d.ts   # 知识库相关类型
└── env.d.ts             # Vite 环境变量类型
```

## 关键机制

### 请求拦截器 (request.ts)

```
请求 → 注入 Authorization: Bearer {token}
响应 → 401: 清除token + 跳转登录
     → 其他错误: message.error() 提示
```

### 路由守卫 (router/index.ts)

```
未登录 → 跳转 /login
已登录 → 放行
/login → 已登录则跳转首页
```

### 状态管理

| Store | 核心状态 | 核心方法 |
|-------|----------|----------|
| user | token, user, isAuthenticated | login(), logout() |
| tenant | currentTenant, members | fetchTenant(), updateTenant() |
| knowledge | knowledgeBase, documents, currentDocument | fetchKnowledge(), uploadDocument() |

## 详细文档

- [pitfalls.md](pitfalls.md)
