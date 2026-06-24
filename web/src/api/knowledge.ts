import request from '@/utils/request'
import type { PageResult, DocumentInfo } from '@/types/api'

/**
 * 知识库相关 API
 */

/** 获取知识库信息 */
export function getKnowledge() {
  return request.get<any, { data: any }>('/api/v1/knowledge')
}

/** 更新知识库信息 */
export function updateKnowledge(data: { name: string; description?: string }) {
  return request.put<any, { data: any }>('/api/v1/knowledge', data)
}

/** 获取文档列表 */
export function listDocuments(params: { page?: number; size?: number; keyword?: string; status?: string }) {
  return request.get<any, { data: PageResult<DocumentInfo> }>('/api/v1/knowledge/documents', { params })
}

/** 上传文档 */
export function uploadDocument(formData: FormData) {
  return request.post<any, { data: { documentId: number; taskId: number; status: string } }>(
    '/api/v1/knowledge/documents',
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
    },
  )
}

/** 删除文档 */
export function deleteDocument(documentId: number) {
  return request.delete<any, any>(`/api/v1/knowledge/documents/${documentId}`)
}

/** 获取文档版本列表 */
export function listVersions(documentId: number) {
  return request.get<any, { data: any[] }>(`/api/v1/knowledge/documents/${documentId}/versions`)
}

/** 版本回滚 */
export function rollbackVersion(documentId: number, versionId: number) {
  return request.post<any, any>(`/api/v1/knowledge/documents/${documentId}/versions/${versionId}/rollback`)
}
