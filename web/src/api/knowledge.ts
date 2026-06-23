import request from '@/utils/request'
import type { R, PageResult, PageParams } from '@/types/api'
import type { KnowledgeDocument, KnowledgeUploadResponse } from '@/types/knowledge'

/**
 * 知识库相关 API
 */

/** 获取文档列表 */
export function getDocuments(params: PageParams) {
  return request.get<R<PageResult<KnowledgeDocument>>>('/knowledge-documents', { params })
}

/** 上传文档 */
export function uploadDocument(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<R<KnowledgeUploadResponse>>('/knowledge-documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 删除文档 */
export function deleteDocument(id: number) {
  return request.delete<R<void>>(`/knowledge-documents/${id}`)
}
