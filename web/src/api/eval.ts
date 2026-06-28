import request from '@/utils/request'

/**
 * 评测集管理
 */

/** 评测集信息 */
export interface EvalSet {
  id: number
  name: string
  pairCount: number
  createdAt: string
}

/** 评测对信息 */
export interface EvalPair {
  id: number
  question: string
  expectedDocument: string
}

/** 评测结果信息 */
export interface EvalResult {
  id: number
  evalSetId: number
  hitRate: number
  totalPairs: number
  hitPairs: number
  status: string
  createdAt: string
}

/** 评测对结果 */
export interface EvalPairResult {
  pairId: number
  question: string
  expectedDocument: string
  hit: boolean
  retrievedDocuments: string[]
}

/** 创建评测集 */
export function createEvalSet(data: { name: string; description?: string }) {
  return request.post<any, { data: EvalSet }>('/api/v1/eval/sets', data)
}

/** 获取评测集列表 */
export function listEvalSets() {
  return request.get<any, { data: EvalSet[] }>('/api/v1/eval/sets')
}

/** 删除评测集 */
export function deleteEvalSet(evalSetId: number) {
  return request.delete<any, any>(`/api/v1/eval/sets/${evalSetId}`)
}

/** 获取评测对列表 */
export function listEvalPairs(evalSetId: number) {
  return request.get<any, { data: EvalPair[] }>(`/api/v1/eval/sets/${evalSetId}/pairs`)
}

/** 添加评测对 */
export function addEvalPair(evalSetId: number, data: { question: string; expectedDocument: string }) {
  return request.post<any, { data: EvalPair }>(`/api/v1/eval/sets/${evalSetId}/pairs`, data)
}

/** 批量导入评测对 */
export function batchImportPairs(evalSetId: number, data: { pairs: { question: string; expectedDocument: string }[] }) {
  return request.post<any, { data: { imported: number } }>(`/api/v1/eval/sets/${evalSetId}/pairs/import`, data)
}

/** 删除评测对 */
export function deleteEvalPair(evalSetId: number, pairId: number) {
  return request.delete<any, any>(`/api/v1/eval/sets/${evalSetId}/pairs/${pairId}`)
}

/** 执行评测 */
export function executeEval(evalSetId: number) {
  return request.post<any, { data: { resultId: number; status: string } }>(`/api/v1/eval/sets/${evalSetId}/run`)
}

/** 获取评测结果列表 */
export function listEvalResults(evalSetId: number) {
  return request.get<any, { data: EvalResult[] }>(`/api/v1/eval/sets/${evalSetId}/results`)
}

/** 获取评测结果详情 */
export function getEvalResultDetail(evalSetId: number, resultId: number) {
  return request.get<any, { data: EvalPairResult[] }>(`/api/v1/eval/sets/${evalSetId}/results/${resultId}`)
}
