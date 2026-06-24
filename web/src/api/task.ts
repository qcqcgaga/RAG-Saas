import request from '@/utils/request'
import type { PageResult, TaskInfo, TaskDetail } from '@/types/api'

/**
 * 异步任务相关 API
 */

/** 获取任务列表 */
export function listTasks(params: { page?: number; size?: number }) {
  return request.get<any, { data: PageResult<TaskInfo> }>('/api/v1/tasks', { params })
}

/** 获取任务详情 */
export function getTask(taskId: number) {
  return request.get<any, { data: TaskDetail }>(`/api/v1/tasks/${taskId}`)
}

/** 重试失败任务 */
export function retryTask(taskId: number) {
  return request.post<any, { data: TaskInfo }>(`/api/v1/tasks/${taskId}/retry`)
}
