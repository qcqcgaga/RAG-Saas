<template>
  <div class="task-view">
    <a-page-header title="任务管理" sub-title="查看文档处理任务状态" />

    <a-table
      :columns="taskColumns"
      :data-source="taskList"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColorMap[record.status] || 'default'">{{ record.status }}</a-tag>
        </template>
        <template v-if="column.key === 'progress'">
          <a-progress :percent="record.progress" size="small" :status="progressStatus(record.status)" />
        </template>
        <template v-if="column.key === 'action'">
          <a-button
            v-if="record.status === 'FAILED'"
            type="link"
            size="small"
            @click="handleRetry(record.id)"
          >重试</a-button>
          <a-button type="link" size="small" @click="showDetail(record.id)">详情</a-button>
        </template>
      </template>
    </a-table>

    <!-- 任务详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="任务详情" :footer="null" width="600px">
      <a-descriptions bordered :column="1" size="small" v-if="taskDetail">
        <a-descriptions-item label="任务ID">{{ taskDetail.id }}</a-descriptions-item>
        <a-descriptions-item label="类型">{{ taskDetail.taskType }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColorMap[taskDetail.status] || 'default'">{{ taskDetail.status }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="进度">
          <a-progress :percent="taskDetail.progress" :status="progressStatus(taskDetail.status)" />
        </a-descriptions-item>
        <a-descriptions-item label="文档名">{{ taskDetail.documentName || '—' }}</a-descriptions-item>
        <a-descriptions-item label="重试次数">{{ taskDetail.retryCount }} / {{ taskDetail.maxRetry }}</a-descriptions-item>
        <a-descriptions-item v-if="taskDetail.errorMessage" label="错误信息">
          <span class="error-text">{{ taskDetail.errorMessage }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ taskDetail.createdAt }}</a-descriptions-item>
        <a-descriptions-item label="开始时间">{{ taskDetail.startedAt || '—' }}</a-descriptions-item>
        <a-descriptions-item label="完成时间">{{ taskDetail.completedAt || '—' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TaskInfo, TaskDetail } from '@/types/api'
import { listTasks, getTask, retryTask } from '@/api/task'

const taskList = ref<TaskInfo[]>([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const detailVisible = ref(false)
const taskDetail = ref<TaskDetail | null>(null)

const statusColorMap: Record<string, string> = {
  PENDING: 'blue',
  RUNNING: 'orange',
  COMPLETED: 'green',
  FAILED: 'red',
}

const taskColumns = [
  { title: '任务ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '类型', dataIndex: 'taskType', key: 'taskType', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '进度', key: 'progress', width: 200 },
  { title: '重试', dataIndex: 'retryCount', key: 'retryCount', width: 80 },
  { title: '操作', key: 'action', width: 140 },
]

function progressStatus(status: string): '' | 'success' | 'exception' | 'active' {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'exception'
  if (status === 'RUNNING') return 'active'
  return ''
}

async function fetchTasks() {
  loading.value = true
  try {
    const res = await listTasks({ page: pagination.current, size: pagination.pageSize })
    taskList.value = res.data.list
    pagination.total = res.data.total
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchTasks()
}

async function showDetail(taskId: number) {
  try {
    const res = await getTask(taskId)
    taskDetail.value = res.data
    detailVisible.value = true
  } catch { /* handled */ }
}

async function handleRetry(taskId: number) {
  try {
    await retryTask(taskId)
    message.success('重试已触发')
    fetchTasks()
  } catch { /* handled */ }
}

onMounted(() => {
  fetchTasks()
})
</script>

<style scoped>
.task-view {
  max-width: 1000px;
}

.error-text {
  color: #ff4d4f;
  word-break: break-all;
}
</style>
