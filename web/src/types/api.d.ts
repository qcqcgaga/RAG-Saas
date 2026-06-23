/**
 * 后端统一响应类型定义
 *
 * 对应后端 R<T> 结构：{ code: number, msg: string, data: T }
 */

/** 统一响应 */
export interface R<T> {
  code: number
  msg: string
  data: T
}

/** 分页结果 */
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
