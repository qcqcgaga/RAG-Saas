<template>
  <div class="stats-view">
    <a-page-header title="用量统计" sub-title="查看 API 调用与 Token 消耗" />

    <!-- 周期选择 -->
    <div class="stats-toolbar">
      <a-radio-group v-model:value="period" button-style="solid" @change="handlePeriodChange">
        <a-radio-button value="7d">近 7 天</a-radio-button>
        <a-radio-button value="30d">近 30 天</a-radio-button>
      </a-radio-group>
    </div>

    <!-- 概览卡片 -->
    <a-row :gutter="16" class="overview-cards">
      <a-col :span="8">
        <a-card>
          <a-statistic title="总调用量" :value="overview.totalCalls" :loading="overviewLoading">
            <template #prefix><ApiOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card>
          <a-statistic title="总 Token 消耗" :value="overview.totalTokens" :loading="overviewLoading">
            <template #prefix><FireOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card>
          <a-statistic title="总对话数" :value="overview.totalConversations" :loading="overviewLoading">
            <template #prefix><MessageOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <!-- 今日用量 -->
    <a-card class="today-card" title="今日用量">
      <div class="today-content">
        <a-statistic title="今日调用" :value="overview.todayCalls" />
        <div class="today-progress">
          <span class="today-label">今日限额: {{ overview.todayLimit }}</span>
          <a-progress
            :percent="todayPercent"
            :status="todayPercent >= 90 ? 'exception' : todayPercent >= 70 ? 'active' : 'normal'"
          />
        </div>
      </div>
    </a-card>

    <!-- 每日统计表格 -->
    <a-card class="daily-card" title="每日统计">
      <a-table
        :columns="dailyColumns"
        :data-source="dailyList"
        :loading="dailyLoading"
        row-key="date"
        :pagination="false"
        size="middle"
      />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ApiOutlined, FireOutlined, MessageOutlined } from '@ant-design/icons-vue'
import { getOverview, getDailyStats } from '@/api/stats'

interface OverviewData {
  totalCalls: number
  totalTokens: number
  totalConversations: number
  todayCalls: number
  todayLimit: number
}

interface DailyItem {
  date: string
  calls: number
  tokens: number
  conversations: number
}

const period = ref('7d')
const overview = reactive<OverviewData>({
  totalCalls: 0,
  totalTokens: 0,
  totalConversations: 0,
  todayCalls: 0,
  todayLimit: 0,
})
const overviewLoading = ref(false)

const dailyList = ref<DailyItem[]>([])
const dailyLoading = ref(false)

const todayPercent = computed(() => {
  if (overview.todayLimit === 0) return 0
  return Math.round((overview.todayCalls / overview.todayLimit) * 100)
})

const dailyColumns = [
  { title: '日期', dataIndex: 'date', key: 'date', width: 140 },
  { title: '调用量', dataIndex: 'calls', key: 'calls', width: 120 },
  { title: 'Token 消耗', dataIndex: 'tokens', key: 'tokens', width: 140 },
  { title: '对话数', dataIndex: 'conversations', key: 'conversations', width: 120 },
]

function getDateRange(): { startDate: string; endDate: string } {
  const end = new Date()
  const start = new Date()
  const days = period.value === '30d' ? 30 : 7
  start.setDate(start.getDate() - days)
  const fmt = (d: Date) => d.toISOString().slice(0, 10)
  return { startDate: fmt(start), endDate: fmt(end) }
}

async function fetchOverview() {
  overviewLoading.value = true
  try {
    const res = await getOverview(period.value)
    Object.assign(overview, res.data)
  } catch { /* handled */ } finally {
    overviewLoading.value = false
  }
}

async function fetchDaily() {
  dailyLoading.value = true
  try {
    const { startDate, endDate } = getDateRange()
    const res = await getDailyStats(startDate, endDate)
    dailyList.value = res.data
  } catch { /* handled */ } finally {
    dailyLoading.value = false
  }
}

function handlePeriodChange() {
  fetchOverview()
  fetchDaily()
}

onMounted(() => {
  fetchOverview()
  fetchDaily()
})
</script>

<style scoped>
.stats-view {
  max-width: 1100px;
}

.stats-toolbar {
  margin-bottom: 16px;
}

.overview-cards {
  margin-bottom: 16px;
}

.today-card {
  margin-bottom: 16px;
}

.today-content {
  display: flex;
  align-items: center;
  gap: 48px;
}

.today-progress {
  flex: 1;
}

.today-label {
  color: #999;
  font-size: 13px;
  display: block;
  margin-bottom: 4px;
}

.daily-card {
  margin-bottom: 16px;
}
</style>
