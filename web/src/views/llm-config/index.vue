<template>
  <div class="llm-config-view">
    <a-page-header title="LLM 配置" sub-title="配置大语言模型 API 连接" />

    <!-- 当前配置状态 -->
    <a-alert
      v-if="config && config.status === 1"
      type="success"
      show-icon
      message="LLM 已配置"
      :description="`模型: ${config.modelName} | API: ${config.apiUrl}`"
      style="margin-bottom: 16px"
    />
    <a-alert
      v-if="config && config.status === 0"
      type="warning"
      show-icon
      message="LLM 未配置"
      description="请配置 API 地址和密钥后使用"
      style="margin-bottom: 16px"
    />

    <!-- 配置表单 -->
    <a-card title="配置信息" style="max-width: 600px">
      <a-form :model="form" layout="vertical" @finish="handleSave">
        <a-form-item label="API 地址" :rules="[{ required: true, message: '请输入 API 地址' }]">
          <a-input v-model:value="form.apiUrl" placeholder="例如: https://api.openai.com/v1/messages" />
        </a-form-item>
        <a-form-item label="API Key">
          <a-input-password
            v-model:value="form.apiKey"
            :placeholder="config?.apiKeyMasked ? '已设置，留空则不修改' : '请输入 API Key'"
          />
        </a-form-item>
        <a-form-item label="模型名称" :rules="[{ required: true, message: '请输入模型名称' }]">
          <a-input v-model:value="form.modelName" placeholder="例如: claude-sonnet-4-20250514" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="saving">保存</a-button>
            <a-button :loading="testing" @click="handleTest">测试连通性</a-button>
            <a-popconfirm title="确定恢复默认配置？当前配置将被清除。" @confirm="handleReset">
              <a-button danger>恢复默认</a-button>
            </a-popconfirm>
          </a-space>
        </a-form-item>
      </a-form>

      <!-- 测试结果 -->
      <div v-if="testResult" class="test-result">
        <a-alert
          :type="testResult.connected ? 'success' : 'error'"
          show-icon
        >
          <template #message>
            {{ testResult.connected ? '连接成功' : '连接失败' }}
          </template>
          <template #description>
            <span v-if="testResult.connected">模型: {{ testResult.modelName }} | 响应时间: {{ testResult.responseTimeMs }}ms</span>
            <span v-else>请检查 API 地址、密钥和模型名称是否正确</span>
          </template>
        </a-alert>
      </div>

      <!-- 最后更新时间 -->
      <div v-if="config?.updatedAt" class="config-meta">
        <span class="meta-label">最后更新: {{ config.updatedAt }}</span>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getLlmConfig, updateLlmConfig, testLlmConfig, deleteLlmConfig } from '@/api/llmConfig'
import type { LlmConfig, LlmTestResult } from '@/api/llmConfig'

const config = ref<LlmConfig | null>(null)
const saving = ref(false)
const testing = ref(false)
const testResult = ref<LlmTestResult | null>(null)

const form = reactive({
  apiUrl: '',
  apiKey: '',
  modelName: '',
})

async function fetchConfig() {
  try {
    const res = await getLlmConfig()
    config.value = res.data
    form.apiUrl = res.data.apiUrl || ''
    form.modelName = res.data.modelName || ''
    form.apiKey = ''
  } catch { /* handled */ }
}

async function handleSave() {
  saving.value = true
  try {
    const data: { apiUrl: string; apiKey: string; modelName: string } = {
      apiUrl: form.apiUrl,
      apiKey: form.apiKey,
      modelName: form.modelName,
    }
    const res = await updateLlmConfig(data)
    config.value = res.data
    form.apiKey = ''
    testResult.value = null
    message.success('配置已保存')
  } catch { /* handled */ } finally {
    saving.value = false
  }
}

async function handleTest() {
  if (!form.apiUrl || !form.modelName) {
    message.warning('请先填写 API 地址和模型名称')
    return
  }
  if (!form.apiKey && !config.value?.apiKeyMasked) {
    message.warning('请先填写 API Key')
    return
  }
  testing.value = true
  testResult.value = null
  try {
    const res = await testLlmConfig({
      apiUrl: form.apiUrl,
      apiKey: form.apiKey || 'use-saved', // 如果用户没改key，用占位符
      modelName: form.modelName,
    })
    testResult.value = res.data
  } catch { /* handled */ } finally {
    testing.value = false
  }
}

async function handleReset() {
  try {
    await deleteLlmConfig()
    message.success('已恢复默认配置')
    testResult.value = null
    fetchConfig()
  } catch { /* handled */ }
}

onMounted(() => {
  fetchConfig()
})
</script>

<style scoped>
.llm-config-view {
  max-width: 800px;
}

.test-result {
  margin-top: 16px;
}

.config-meta {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.meta-label {
  color: #999;
  font-size: 13px;
}
</style>
