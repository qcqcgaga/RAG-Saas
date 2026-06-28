<template>
  <div class="apikey-view">
    <a-page-header title="API Key 管理" sub-title="管理租户的 API 访问密钥" />

    <div class="apikey-toolbar">
      <a-button type="primary" @click="showCreateModal">
        <template #icon><PlusOutlined /></template>
        创建 Key
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="keyList"
      :loading="loading"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'keyMasked'">
          <a-typography-text code>{{ record.keyMasked }}</a-typography-text>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">
            {{ record.status === 1 ? '有效' : '已吊销' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'dailyLimit'">
          {{ record.dailyLimit ?? 1000 }}
        </template>
        <template v-if="column.key === 'lastUsedAt'">
          {{ record.lastUsedAt || '—' }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="showRenameModal(record)">重命名</a-button>
            <a-popconfirm
              title="确定吊销该 Key？吊销后不可恢复。"
              @confirm="handleRevoke(record.id)"
            >
              <a-button type="link" danger size="small">吊销</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 创建 Key 弹窗 -->
    <a-modal v-model:open="createVisible" title="创建 API Key" @ok="handleCreate">
      <a-form layout="vertical">
        <a-form-item label="名称（可选）">
          <a-input v-model:value="createForm.name" placeholder="例如：生产环境" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 展示新 Key 弹窗 -->
    <a-modal
      v-model:open="keyShowVisible"
      title="API Key 已创建"
      :closable="false"
      :maskClosable="false"
      :okText="'我已保存'"
      @ok="keyShowVisible = false"
    >
      <a-alert
        type="warning"
        show-icon
        message="此密钥仅展示一次，请立即复制保存"
        style="margin-bottom: 16px"
      />
      <a-input-group compact>
        <a-input :value="newFullKey" read-only style="flex: 1" />
        <a-button type="primary" @click="handleCopyKey">复制</a-button>
      </a-input-group>
    </a-modal>

    <!-- 重命名弹窗 -->
    <a-modal v-model:open="renameVisible" title="重命名 API Key" @ok="handleRename">
      <a-form layout="vertical">
        <a-form-item label="名称">
          <a-input v-model:value="renameForm.name" placeholder="输入新名称" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { createApiKey, listApiKeys, revokeApiKey, renameApiKey } from '@/api/apikey'
import type { ApiKeyData } from '@/api/apikey'

const keyList = ref<ApiKeyData[]>([])
const loading = ref(false)

const createVisible = ref(false)
const createForm = reactive({ name: '' })

const keyShowVisible = ref(false)
const newFullKey = ref('')

const renameVisible = ref(false)
const renameForm = reactive({ id: 0, name: '' })

const columns = [
  { title: 'Key', key: 'keyMasked', width: 200 },
  { title: '名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '状态', key: 'status', width: 100 },
  { title: '每日限额', key: 'dailyLimit', width: 100 },
  { title: '最后使用', key: 'lastUsedAt', width: 170 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 160 },
]

async function fetchKeys() {
  loading.value = true
  try {
    const res = await listApiKeys()
    keyList.value = res.data
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

function showCreateModal() {
  createForm.name = ''
  createVisible.value = true
}

async function handleCreate() {
  try {
    const res = await createApiKey({ name: createForm.name || undefined })
    // 后端 ApiKeyResponse.key 字段包含完整 Key
    newFullKey.value = res.data.key || ''
    createVisible.value = false
    keyShowVisible.value = true
    fetchKeys()
  } catch { /* handled */ }
}

function handleCopyKey() {
  navigator.clipboard.writeText(newFullKey.value).then(() => {
    message.success('已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败，请手动复制')
  })
}

function showRenameModal(record: ApiKeyData) {
  renameForm.id = record.id
  renameForm.name = record.name
  renameVisible.value = true
}

async function handleRename() {
  try {
    await renameApiKey(renameForm.id, renameForm.name)
    renameVisible.value = false
    message.success('重命名成功')
    fetchKeys()
  } catch { /* handled */ }
}

async function handleRevoke(keyId: number) {
  try {
    await revokeApiKey(keyId)
    message.success('已吊销')
    fetchKeys()
  } catch { /* handled */ }
}

onMounted(() => {
  fetchKeys()
})
</script>

<style scoped>
.apikey-view {
  max-width: 1000px;
}

.apikey-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}
</style>
