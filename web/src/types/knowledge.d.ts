/** 知识库相关类型定义 */

export interface KnowledgeDocument {
  id: number
  title: string
  fileName: string
  fileType: string
  fileSize: number
  version: number
  status: 'pending' | 'processing' | 'ready' | 'failed'
  createdAt: string
  updatedAt: string
}

export interface KnowledgeUploadResponse {
  documentId: number
  taskId: number
}
