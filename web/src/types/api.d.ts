/** 统一响应格式 */
export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
}

/** 分页响应 */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

/** 分页请求参数 */
export interface PageParams {
  page: number
  size: number
}

/** 认证响应 */
export interface AuthData {
  userId: number
  tenantId: number
  role: string
  token: string
  expiresIn: number
}

/** 租户信息 */
export interface TenantInfo {
  id: number
  name: string
  slug: string
  status: number
  memberCount: number
  documentCount: number
  createdAt: string
}

/** 成员信息 */
export interface MemberInfo {
  userId: number
  email: string
  displayName: string | null
  role: string
  status: number
  createdAt: string
}

/** 文档信息 */
export interface DocumentInfo {
  id: number
  originalName: string
  fileType: string
  fileSize: number
  status: string
  chunkCount: number
  latestTaskId: number | null
  createdAt: string
  updatedAt: string
}

/** 任务信息 */
export interface TaskInfo {
  id: number
  taskType: string
  status: string
  progress: number
  retryCount: number
  maxRetry: number
}

/** 任务详情 */
export interface TaskDetail extends TaskInfo {
  documentId: number | null
  documentName: string | null
  errorMessage: string | null
  startedAt: string | null
  completedAt: string | null
  createdAt: string
}

/** Widget 配置 */
export interface WidgetConfig {
  brandColor: string
  welcomeMessage: string
  iconUrl: string | null
  enabled: boolean
}
