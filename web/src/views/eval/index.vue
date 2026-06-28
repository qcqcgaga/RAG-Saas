<template>
  <div class="eval-view">
    <a-page-header title="评测集" sub-title="管理评测数据集与执行评测" />

    <a-row :gutter="24">
      <!-- 左侧：评测集列表 -->
      <a-col :span="10">
        <div class="eval-set-toolbar">
          <a-button type="primary" @click="showCreateSetModal">
            <template #icon><PlusOutlined /></template>
            创建评测集
          </a-button>
        </div>

        <a-table
          :columns="setColumns"
          :data-source="evalSets"
          :loading="setLoading"
          row-key="id"
          :custom-row="setCustomRow"
          :row-class-name="(_record: any) => _record?.id === selectedSetId ? 'selected-row' : ''"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-popconfirm title="确定删除该评测集？" @confirm="handleDeleteSet(record.id)">
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </a-col>

      <!-- 右侧：评测集详情 -->
      <a-col :span="14">
        <div v-if="!selectedSetId" class="eval-detail-empty">
          <a-empty description="请选择一个评测集" />
        </div>

        <div v-else class="eval-detail">
          <div class="eval-detail-header">
            <h3>{{ selectedSetName }}</h3>
            <a-space>
              <a-button @click="showBatchImportModal">
                <template #icon><ImportOutlined /></template>
                批量导入
              </a-button>
              <a-button type="primary" :loading="executing" @click="handleExecute">
                <template #icon><PlayCircleOutlined /></template>
                执行评测
              </a-button>
              <a-tooltip title="如果评测卡在'已在执行中'，点击强制清理锁">
                <a-button @click="handleForceClearLock">
                  <template #icon><UnlockOutlined /></template>
                  清理锁
                </a-button>
              </a-tooltip>
            </a-space>
          </div>

          <!-- 评测对列表 -->
          <div class="pair-section">
            <div class="pair-toolbar">
              <span>评测对 ({{ pairs.length }})</span>
              <a-button size="small" @click="showAddPairModal">
                <template #icon><PlusOutlined /></template>
                添加
              </a-button>
            </div>
            <a-table
              :columns="pairColumns"
              :data-source="pairs"
              :loading="pairLoading"
              row-key="id"
              size="small"
              :pagination="false"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'action'">
                  <a-popconfirm title="确定删除该评测对？" @confirm="handleDeletePair(record.id)">
                    <a-button type="link" danger size="small">删除</a-button>
                  </a-popconfirm>
                </template>
              </template>
            </a-table>
          </div>

          <!-- 评测结果列表 -->
          <div class="result-section">
            <h4>评测结果</h4>
            <a-table
              :columns="resultColumns"
              :data-source="results"
              :loading="resultLoading"
              row-key="id"
              size="small"
              :pagination="false"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'hitRate'">
                  <a-progress :percent="Math.round(record.hitRate)" size="small" />
                </template>
                <template v-if="column.key === 'status'">
                  <a-tag :color="record.status === 'COMPLETED' ? 'green' : record.status === 'RUNNING' ? 'orange' : 'blue'">
                    {{ record.status }}
                  </a-tag>
                </template>
                <template v-if="column.key === 'action'">
                  <a-button type="link" size="small" @click="showResultDetail(record.id)">详情</a-button>
                </template>
              </template>
            </a-table>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 创建评测集弹窗 -->
    <a-modal v-model:open="createSetVisible" title="创建评测集" @ok="handleCreateSet">
      <a-form layout="vertical">
        <a-form-item label="名称" :rules="[{ required: true, message: '请输入名称' }]">
          <a-input v-model:value="createSetForm.name" placeholder="例如：产品FAQ评测" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 添加评测对弹窗 -->
    <a-modal v-model:open="addPairVisible" title="添加评测对" @ok="handleAddPair">
      <a-form layout="vertical">
        <a-form-item label="问题" :rules="[{ required: true, message: '请输入问题' }]">
          <a-textarea v-model:value="addPairForm.question" :rows="2" placeholder="输入测试问题" />
        </a-form-item>
        <a-form-item label="期望文档" :rules="[{ required: true, message: '请输入期望文档标识' }]">
          <a-input v-model:value="addPairForm.expectedDocument" placeholder="输入期望命中的文档标识" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 批量导入弹窗 -->
    <a-modal v-model:open="batchImportVisible" title="批量导入评测对" @ok="handleBatchImport" width="600px">
      <a-alert type="info" show-icon style="margin-bottom: 12px">
        <template #message>
          请输入 JSON 数组，格式: [{"question": "...", "expectedDocument": "..."}]
        </template>
      </a-alert>
      <a-textarea v-model:value="batchImportJson" :rows="8" placeholder='[{"question": "如何重置密码？", "expectedDocument": "密码重置指南"}]' />
    </a-modal>

    <!-- 评测结果详情弹窗 -->
    <a-modal v-model:open="resultDetailVisible" title="评测结果详情" :footer="null" width="700px">
      <a-table
        :columns="pairResultColumns"
        :data-source="pairResults"
        :loading="pairResultLoading"
        row-key="pairId"
        size="small"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'hit'">
            <a-tag :color="record.hit ? 'green' : 'red'">{{ record.hit ? '命中' : '未命中' }}</a-tag>
          </template>
          <template v-if="column.key === 'retrievedDocuments'">
            <span>{{ record.retrievedDocuments?.join(', ') || '—' }}</span>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ImportOutlined, PlayCircleOutlined, UnlockOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'
import {
  createEvalSet,
  listEvalSets,
  deleteEvalSet,
  listEvalPairs,
  addEvalPair,
  batchImportPairs,
  deleteEvalPair,
  executeEval,
  listEvalResults,
  getEvalResultDetail,
} from '@/api/eval'
import type { EvalSet, EvalPair, EvalResult, EvalPairResult } from '@/api/eval'

const evalSets = ref<EvalSet[]>([])
const setLoading = ref(false)
const selectedSetId = ref<number | null>(null)
const selectedSetName = ref('')

const pairs = ref<EvalPair[]>([])
const pairLoading = ref(false)

const results = ref<EvalResult[]>([])
const resultLoading = ref(false)
const executing = ref(false)

// 创建评测集
const createSetVisible = ref(false)
const createSetForm = reactive({ name: '' })

// 添加评测对
const addPairVisible = ref(false)
const addPairForm = reactive({ question: '', expectedDocument: '' })

// 批量导入
const batchImportVisible = ref(false)
const batchImportJson = ref('')

// 结果详情
const resultDetailVisible = ref(false)
const pairResults = ref<EvalPairResult[]>([])
const pairResultLoading = ref(false)

const setColumns = [
  { title: '名称', dataIndex: 'name', key: 'name', ellipsis: true },
  { title: '评测对数', dataIndex: 'pairCount', key: 'pairCount', width: 90 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 150 },
  { title: '操作', key: 'action', width: 70 },
]

const pairColumns = [
  { title: '问题', dataIndex: 'question', key: 'question', ellipsis: true },
  { title: '期望文档', dataIndex: 'expectedDocument', key: 'expectedDocument', ellipsis: true },
  { title: '操作', key: 'action', width: 70 },
]

const resultColumns = [
  { title: '命中率', key: 'hitRate', width: 160 },
  { title: '命中/总数', key: 'hitTotal', customRender: ({ record }: any) => `${record.hitPairs}/${record.totalPairs}`, width: 100 },
  { title: '状态', key: 'status', width: 100 },
  { title: '执行时间', dataIndex: 'createdAt', key: 'createdAt', width: 150 },
  { title: '操作', key: 'action', width: 70 },
]

const pairResultColumns = [
  { title: '问题', dataIndex: 'question', key: 'question', ellipsis: true },
  { title: '期望文档', dataIndex: 'expectedDocument', key: 'expectedDocument', ellipsis: true },
  { title: '结果', key: 'hit', width: 80 },
  { title: '检索文档', key: 'retrievedDocuments' },
]

function setCustomRow(record: EvalSet) {
  return {
    onClick: () => {
      selectedSetId.value = record.id
      selectedSetName.value = record.name
      fetchPairs(record.id)
      fetchResults(record.id)
    },
    style: { cursor: 'pointer' },
  }
}

async function fetchEvalSets() {
  setLoading.value = true
  try {
    const res = await listEvalSets()
    evalSets.value = res.data
  } catch { /* handled */ } finally {
    setLoading.value = false
  }
}

async function fetchPairs(evalSetId: number) {
  pairLoading.value = true
  try {
    const res = await listEvalPairs(evalSetId)
    pairs.value = res.data
  } catch { /* handled */ } finally {
    pairLoading.value = false
  }
}

async function fetchResults(evalSetId: number) {
  resultLoading.value = true
  try {
    const res = await listEvalResults(evalSetId)
    results.value = res.data
  } catch { /* handled */ } finally {
    resultLoading.value = false
  }
}

function showCreateSetModal() {
  createSetForm.name = ''
  createSetVisible.value = true
}

async function handleCreateSet() {
  try {
    await createEvalSet({ name: createSetForm.name })
    createSetVisible.value = false
    message.success('评测集已创建')
    fetchEvalSets()
  } catch { /* handled */ }
}

async function handleDeleteSet(id: number) {
  try {
    await deleteEvalSet(id)
    message.success('已删除')
    if (selectedSetId.value === id) {
      selectedSetId.value = null
      selectedSetName.value = ''
      pairs.value = []
      results.value = []
    }
    fetchEvalSets()
  } catch { /* handled */ }
}

function showAddPairModal() {
  addPairForm.question = ''
  addPairForm.expectedDocument = ''
  addPairVisible.value = true
}

async function handleAddPair() {
  if (!selectedSetId.value) return
  try {
    await addEvalPair(selectedSetId.value, { question: addPairForm.question, expectedDocument: addPairForm.expectedDocument })
    addPairVisible.value = false
    message.success('已添加')
    fetchPairs(selectedSetId.value)
    fetchEvalSets()
  } catch { /* handled */ }
}

function showBatchImportModal() {
  batchImportJson.value = ''
  batchImportVisible.value = true
}

async function handleBatchImport() {
  if (!selectedSetId.value) return
  try {
    const parsed = JSON.parse(batchImportJson.value)
    if (!Array.isArray(parsed)) {
      message.error('请输入 JSON 数组')
      return
    }
    // 验证数组中每项是否包含必需字段
    for (let i = 0; i < parsed.length; i++) {
      const item = parsed[i]
      if (!item.question || typeof item.question !== 'string' || !item.question.trim()) {
        message.error(`第 ${i + 1} 项缺少 question 字段或为空`)
        return
      }
      if (!item.expectedDocument || typeof item.expectedDocument !== 'string' || !item.expectedDocument.trim()) {
        message.error(`第 ${i + 1} 项缺少 expectedDocument 字段或为空`)
        return
      }
    }
    const res = await batchImportPairs(selectedSetId.value, { pairs: parsed })
    batchImportVisible.value = false
    message.success(`已导入 ${res.data.imported} 条`)
    fetchPairs(selectedSetId.value)
    fetchEvalSets()
  } catch (e: any) {
    if (e instanceof SyntaxError) {
      message.error('JSON 格式不正确，请检查：逗号分隔、引号闭合、无多余换行等')
    } else {
      message.error(e?.message || '导入失败，请检查数据格式')
    }
  }
}

async function handleDeletePair(pairId: number) {
  if (!selectedSetId.value) return
  try {
    await deleteEvalPair(selectedSetId.value, pairId)
    message.success('已删除')
    fetchPairs(selectedSetId.value)
    fetchEvalSets()
  } catch { /* handled */ }
}

async function handleExecute() {
  if (!selectedSetId.value) return
  executing.value = true
  try {
    await executeEval(selectedSetId.value)
    message.success('评测已启动')
    // 轮询结果
    pollResults(selectedSetId.value)
  } catch (e: any) {
    // 显示评测启动错误（如"评测已在执行中"、"评测集无问答对"等）
    const msg = e?.message || '评测启动失败'
    if (!msg.includes('已在执行中')) {
      message.error(msg)
    } else {
      message.warning(msg)
    }
  } finally {
    executing.value = false
  }
}

function pollResults(evalSetId: number) {
  let count = 0
  const timer = setInterval(async () => {
    count++
    if (count > 30) {
      clearInterval(timer)
      return
    }
    try {
      const res = await listEvalResults(evalSetId)
      results.value = res.data
      const latest = res.data[0]
      if (latest && latest.status !== 'RUNNING' && latest.status !== 'PENDING') {
        clearInterval(timer)
      }
    } catch {
      clearInterval(timer)
    }
  }, 2000)
}

async function handleForceClearLock() {
  if (!selectedSetId.value) return
  try {
    // 直接调用后端清理锁接口
    await request.delete(`/api/v1/eval/sets/${selectedSetId.value}/lock`)
    message.success('评测锁已清理，可以重新执行评测')
  } catch {
    message.error('清理锁失败')
  }
}

async function showResultDetail(resultId: number) {
  if (!selectedSetId.value) return
  resultDetailVisible.value = true
  pairResultLoading.value = true
  try {
    const res = await getEvalResultDetail(selectedSetId.value, resultId)
    pairResults.value = res.data
  } catch { /* handled */ } finally {
    pairResultLoading.value = false
  }
}

onMounted(() => {
  fetchEvalSets()
})
</script>

<style scoped>
.eval-view {
  max-width: 1200px;
}

.eval-set-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.eval-detail-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.eval-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.eval-detail-header h3 {
  margin: 0;
  font-size: 16px;
}

.pair-section {
  margin-bottom: 24px;
}

.pair-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
}

.result-section h4 {
  margin-bottom: 12px;
  font-size: 14px;
}

:deep(.selected-row) {
  background-color: #e6f7ff;
}
</style>
