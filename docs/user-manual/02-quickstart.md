# 快速开始

## 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| Java | 21+ | LTS 版本，支持虚拟线程 |
| Maven | 3.9+ | 后端构建工具 |
| Node.js | 18+ | 前端和组件构建 |
| pnpm | 9+ | 前端包管理器 |
| Docker | 24+ | 容器化部署（推荐） |
| Docker Compose | V2+ | 服务编排 |

## 安装部署

### 方式一：Docker Compose（推荐）

Docker Compose 一键启动所有服务，包括 PostgreSQL、Redis、Milvus（含 etcd 和 MinIO）、后端和前端。

1. **克隆项目**

   ```bash
   git clone <repository-url>
   cd RAG-Saas
   ```

2. **配置环境变量**（可选）

   创建 `.env` 文件设置敏感配置：

   ```bash
   # .env
   POSTGRES_PASSWORD=your_secure_password
   JWT_SECRET=your_jwt_secret_at_least_32_characters_long
   XUNFEI_API_KEY=your_xunfei_api_key
   ```

   > **安全提示**：生产环境务必修改默认密码和密钥，不要使用默认值。

3. **启动所有服务**

   ```bash
   docker compose up -d
   ```

4. **验证服务状态**

   ```bash
   docker compose ps
   ```

   所有服务状态应为 `healthy` 或 `running`：

   | 服务 | 端口 | 健康检查 |
   |------|------|----------|
   | PostgreSQL | 5432 | `pg_isready` |
   | Redis | 6379 | `redis-cli ping` |
   | Milvus | 19530 | HTTP `/healthz` |
   | 后端服务 | 8080 | 应用启动日志 |
   | 前端界面 | 80 | HTTP 响应 |

### 方式二：手动部署

适用于开发调试场景，需要分别启动各组件。

1. **启动基础设施**

   ```bash
   # 仅启动 PostgreSQL + Redis + Milvus
   docker compose up -d postgres redis etcd minio milvus
   ```

2. **启动后端**

   ```bash
   cd server
   mvn spring-boot:run
   ```

   后端将在 `http://localhost:8080` 启动。

3. **启动前端**

   ```bash
   cd web
   pnpm install
   pnpm dev
   ```

   前端开发服务器将在 `http://localhost:5173` 启动。

## 首次使用

### 1. 注册账号

1. 打开浏览器访问管理后台地址
   - Docker 部署：`http://localhost`
   - 手动部署：`http://localhost:5173`
2. 在登录页面点击 **注册**
3. 填写注册信息：
   - **邮箱**：用于登录和接收邀请
   - **密码**：至少 8 位，必须包含大写字母、小写字母和数字
   - **租户名称**：您的工作空间名称
   - **租户标识（slug）**：用于唯一标识，仅支持字母、数字和连字符
4. 点击 **确认注册**

[截图：注册页面]

### 2. 上传文档

1. 登录后自动进入 **知识库** 页面
2. 点击 **上传文档** 按钮
3. 选择文件（支持 PDF、Markdown、纯文本格式，单文件最大 50MB）
4. 点击 **确认上传**

[截图：知识库上传界面]

### 3. 获取嵌入脚本

1. 进入 **聊天组件** 页面
2. 查看自动生成的嵌入脚本代码
3. 复制脚本代码，粘贴到您网站的 HTML 中

[截图：聊天组件嵌入脚本界面]

### 4. 验证聊天组件

1. 打开嵌入了脚本的网页
2. 页面右下角应出现聊天图标
3. 点击图标打开聊天窗口
4. 输入问题，验证是否获得基于文档的回答

[截图：聊天组件对话界面]

## 端口和服务清单

| 服务 | 端口 | 访问地址 |
|------|------|----------|
| 管理后台（Nginx） | 80 | `http://localhost` |
| 后端 API | 8080 | `http://localhost:8080` |
| PostgreSQL | 5432 | `localhost:5432` |
| Redis | 6379 | `localhost:6379` |
| Milvus | 19530 | `localhost:19530` |
| MinIO 控制台 | 9001 | `http://localhost:9001` |
