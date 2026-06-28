import request from '@/utils/request'

/**
 * 用量统计
 */

/** 获取概览数据 */
export function getOverview(period: string = '7d') {
  return request.get<any, { data: { totalCalls: number; totalTokens: number; totalConversations: number; todayCalls: number; todayLimit: number } }>('/api/v1/stats/overview', { params: { period } })
}

/** 获取每日统计 */
export function getDailyStats(startDate: string, endDate: string) {
  return request.get<any, { data: { date: string; calls: number; tokens: number; conversations: number }[] }>('/api/v1/stats/daily', { params: { startDate, endDate } })
}

/** 获取趋势数据 */
export function getTrend(period: string = '7d', metric: string = 'calls') {
  return request.get<any, { data: { date: string; value: number }[] }>('/api/v1/stats/trend', { params: { period, metric } })
}
