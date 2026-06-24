<template>
  <div class="knowledge-view">
    <a-page-header title="知识库管理" sub-title="管理文档与知识库配置" />

    <!-- 知识库信息 -->
    <a-descriptions bordered :column="2" class="knowledge-info" size="middle">
      <a-descriptions-item label="名称">{{ knowledgeInfo?.name }}</a-descriptions-item>
      <a-descriptions-item label="描述">{{ knowledgeInfo?.description || '—' }}</a-descriptions-item>
      <a-descriptions-item label="操作">
        <a-button type="link" @click="showEditModal">编辑</a-button>
      </a-descriptions-item>
    </a-descriptions>

    <!-- 文档工具栏 -->
    <div class="doc-toolbar">
      <div class="doc-toolbar-left">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索文档"
          style="width: 240px"
          allow-clear
          @search="handleSearch"
        />
        <a-select
          v-model:value="statusFilter"
          placeholder="状态筛选"
          allow-clear
          style="width: 140px"
          @change="handleSearch"
        >
          <a-select-option value="PENDING">待处理</a-select-option>
          <a-select-option value="PROCESSING">处理中</a-select-option>
          <a-select-option value="COMPLETED">已完成</a-select-option>
          <a-select-option value="FAILED">失败</a-select-option>
        </a-select>
      </div>
      <a-upload
        :before-upload="handleBeforeUpload"
        :show-upload-list="false"
        accept=".pdf,.doc,.docx,.txt,.md,.csv"
        multiple
      >
        <a-button type="primary">
          <template #icon><UploadOutlined /></template>
          上传文档
        </a-button>
      </a-upload>
    </div>

    <!-- 文档表格 -->
    <a-table
      :columns="docColumns"
      :data-source="docList"
      :loading="docLoading"
      :pagination="docPagination"
      row-key="id"
      :expanded-row-keys="expandedRowKeys"
      @change="handleDocTableChange"
      @expand="handleExpand"
    >
      <template #expandedRowRender="{ record }">
        <DocumentVersionPanel :document-id="record.id" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'fileSize'">
          {{ formatFileSize(record.fileSize) }}
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColorMap[record.status]">{{ record.status }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-popconfirm title="确定删除该文档？" @confirm="handleDelete(record.id)">
            <a-button type="link" danger size="small">删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <!-- 编辑知识库弹窗 -->
    <a-modal v-model:open="editVisible" title="编辑知识库" @ok="handleEditKnowledge">
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="名称" :rules="[{ required: true, message: '请输入名称' }]">
          <a-input v-model:value="editForm.name" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="editForm.description" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import type { DocumentInfo } from '@/types/api'
import {
  getKnowledge,
  updateKnowledge,
  listDocuments,
  uploadDocument,
  deleteDocument,
} from '@/api/knowledge'
import DocumentVersionPanel from './DocumentVersionPanel.vue'

const knowledgeInfo = ref<any>(null)
const docList = ref<DocumentInfo[]>([])
const docLoading = ref(false)
const docPagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchKeyword = ref('')
const statusFilter = ref<string | undefined>(undefined)
const expandedRowKeys = ref<number[]>([])

const editVisible = ref(false)
const editForm = reactive({ name: '', description: '' })

const statusColorMap: Record<string, string> = {
  PENDING: 'blue',
  PROCESSING: 'orange',
  COMPLETED: 'green',
  FAILED: 'red',
}

const docColumns = [
  { title: '文件名', dataIndex: 'originalName', key: 'originalName', ellipsis: true },
  { title: '类型', dataIndex: 'fileType', key: 'fileType', width: 80 },
  { title: '大小', key: 'fileSize', width: 100 },
  { title: '状态', key: 'status', width: 100 },
  { title: '分块数', dataIndex: 'chunkCount', key: 'chunkCount', width: 80 },
  { title: '上传时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 80 },
]

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function fetchKnowledge() {
  try {
    const res = await getKnowledge()
    knowledgeInfo.value = res.data
  } catch { /* handled */ }
}

async function fetchDocuments() {
  docLoading.value = true
  try {
    const res = await listDocuments({
      page: docPagination.current,
      size: docPagination.pageSize,
      keyword: searchKeyword.value || undefined,
      status: statusFilter.value || undefined,
    })
    docList.value = res.data.list
    docPagination.total = res.data.total
  } catch { /* handled */ } finally {
    docLoading.value = false
  }
}

function handleSearch() {
  docPagination.current = 1
  fetchDocuments()
}

function handleDocTableChange(pagination: any) {
  docPagination.current = pagination.current
  docPagination.pageSize = pagination.pageSize
  fetchDocuments()
}

function handleExpand(expanded: boolean, record: DocumentInfo) {
  expandedRowKeys.value = expanded ? [record.id] : []
}

async function handleBeforeUpload(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  try {
    await uploadDocument(formData)
    message.success(`${file.name} 上传成功`)
    fetchDocuments()
  } catch { /* handled */ }
  return false
}

async function handleDelete(id: number) {
  try {
    await deleteDocument(id)
    message.success('已删除')
    fetchDocuments()
  } catch { /* handled */ }
}

function showEditModal() {
  editForm.name = knowledgeInfo.value?.name || ''
  editForm.description = knowledgeInfo.value?.description || ''
  editVisible.value = true
}

async function handleEditKnowledge() {
  try {
    const res = await updateKnowledge({ name: editForm.name, description: editForm.description })
    knowledgeInfo.value = res.data
    editVisible.value = false
    message.success('更新成功')
  } catch { /* handled */ }
}

onMounted(() => {
  fetchKnowledge()
  fetchDocuments()
})
</script>

<style scoped>
.knowledge-view {
  max-width: 1100px;
}

.knowledge-info {
  margin-bottom: 24px;
}

.doc-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.doc-toolbar-left {
  display: flex;
  gap: 12px;
}
</style>
