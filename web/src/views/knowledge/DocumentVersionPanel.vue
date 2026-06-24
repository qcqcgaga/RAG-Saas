<template>
  <div class="version-panel">
    <a-spin :spinning="loading">
      <a-table
        v-if="versions.length > 0"
        :columns="versionColumns"
        :data-source="versions"
        row-key="id"
        size="small"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-popconfirm title="确定回滚到此版本？" @confirm="handleRollback(record.id)">
              <a-button type="link" size="small">回滚</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
      <a-empty v-else description="暂无版本记录" />
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { listVersions, rollbackVersion } from '@/api/knowledge'

const props = defineProps<{ documentId: number }>()

const versions = ref<any[]>([])
const loading = ref(false)

const versionColumns = [
  { title: '版本ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '文件名', dataIndex: 'originalName', key: 'originalName', ellipsis: true },
  { title: '大小', dataIndex: 'fileSize', key: 'fileSize', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 80 },
]

async function fetchVersions() {
  loading.value = true
  try {
    const res = await listVersions(props.documentId)
    versions.value = res.data || []
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

async function handleRollback(versionId: number) {
  try {
    await rollbackVersion(props.documentId, versionId)
    message.success('回滚成功')
    fetchVersions()
  } catch { /* handled */ }
}

onMounted(() => {
  fetchVersions()
})
</script>

<style scoped>
.version-panel {
  padding: 8px 0;
}
</style>
