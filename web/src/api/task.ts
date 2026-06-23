import request from '@/utils/request'
import type { R } from '@/types/api'

/**
 * 异步任务相关 API
 */

export interface TaskInfo {
  id: number
  type: string
  status: 'pending' | 'running' | 'completed' | 'failed'
  progress: number
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

/** 查询任务状态 */
export function getTaskStatus(taskId: number) {
  return request.get<R<TaskInfo>>(`/tasks/${taskId}`)
}

/** 重试失败任务 */
export function retryTask(taskId: number) {
  return request.post<R<TaskInfo>>(`/tasks/${taskId}/retry`)
}
