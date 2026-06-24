<template>
  <div class="widget-view">
    <a-page-header title="聊天组件配置" sub-title="自定义嵌入网站的聊天组件" />

    <a-row :gutter="24">
      <!-- 配置表单 -->
      <a-col :span="14">
        <a-form :model="form" layout="vertical" @finish="handleSave">
          <a-form-item label="启用组件">
            <a-switch v-model:checked="form.enabled" />
          </a-form-item>
          <a-form-item label="品牌色">
            <a-input v-model:value="form.brandColor" type="color" style="width: 80px; height: 36px; padding: 4px" />
            <span class="color-hint">{{ form.brandColor }}</span>
          </a-form-item>
          <a-form-item label="欢迎语">
            <a-textarea v-model:value="form.welcomeMessage" :rows="3" placeholder="设置访客看到的欢迎语" />
          </a-form-item>
          <a-form-item label="图标URL">
            <a-input v-model:value="form.iconUrl" placeholder="输入图标图片地址（可选）" />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" html-type="submit" :loading="saving">保存配置</a-button>
          </a-form-item>
        </a-form>

        <!-- 嵌入脚本 -->
        <div class="embed-section" v-if="embedScript">
          <h4>嵌入脚本</h4>
          <a-typography-paragraph :copyable="{ text: embedScript }" :content="embedScript" code />
        </div>

        <div class="token-section">
          <a-button type="dashed" danger @click="handleRegenerateToken">重新生成 Token</a-button>
          <span class="token-hint">生成后需更新嵌入脚本中的 Token</span>
        </div>
      </a-col>

      <!-- 实时预览 -->
      <a-col :span="10">
        <div class="preview-card">
          <h4>组件预览</h4>
          <div class="preview-container">
            <div class="preview-widget" :style="{ borderColor: form.brandColor }">
              <div class="preview-header" :style="{ backgroundColor: form.brandColor }">
                <img v-if="form.iconUrl" :src="form.iconUrl" class="preview-icon" />
                <span class="preview-title">DocChat</span>
              </div>
              <div class="preview-body">
                <div class="preview-bubble" :style="{ borderColor: form.brandColor }">
                  {{ form.welcomeMessage || '你好，有什么可以帮您？' }}
                </div>
              </div>
              <div class="preview-input">
                <a-input placeholder="输入消息..." disabled size="small" />
              </div>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { WidgetConfig } from '@/types/api'
import { getWidgetConfig, updateWidgetConfig, getEmbedScript, regenerateToken } from '@/api/widget'

const form = reactive<WidgetConfig>({
  brandColor: '#1890ff',
  welcomeMessage: '',
  iconUrl: null,
  enabled: true,
})

const saving = ref(false)
const embedScript = ref('')
const widgetToken = ref('')

async function fetchConfig() {
  try {
    // Use stored token or fetch config
    const storedToken = localStorage.getItem('widgetToken') || ''
    if (storedToken) {
      widgetToken.value = storedToken
      const res = await getWidgetConfig(storedToken)
      Object.assign(form, res.data)
    }
  } catch { /* handled */ }
}

async function fetchEmbedScript() {
  try {
    const res = await getEmbedScript()
    embedScript.value = res.data.script
  } catch { /* handled */ }
}

async function handleSave() {
  saving.value = true
  try {
    const res = await updateWidgetConfig({ ...form })
    Object.assign(form, res.data)
    message.success('配置已保存')
  } catch { /* handled */ } finally {
    saving.value = false
  }
}

function handleRegenerateToken() {
  Modal.confirm({
    title: '重新生成 Token',
    content: '生成后旧 Token 将失效，需要更新所有嵌入脚本。确定继续？',
    async onOk() {
      try {
        const res = await regenerateToken()
        widgetToken.value = res.data.token
        localStorage.setItem('widgetToken', res.data.token)
        message.success('Token 已重新生成')
        fetchEmbedScript()
      } catch { /* handled */ }
    },
  })
}

onMounted(() => {
  fetchConfig()
  fetchEmbedScript()
})
</script>

<style scoped>
.widget-view {
  max-width: 1100px;
}

.color-hint {
  margin-left: 12px;
  color: #999;
  font-size: 13px;
}

.embed-section {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.embed-section h4 {
  margin-bottom: 8px;
}

.token-section {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.token-hint {
  color: #999;
  font-size: 13px;
}

.preview-card {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
  position: sticky;
  top: 24px;
}

.preview-card h4 {
  margin-bottom: 12px;
}

.preview-container {
  display: flex;
  justify-content: center;
  padding: 16px 0;
}

.preview-widget {
  width: 300px;
  border: 2px solid #1890ff;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.preview-header {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  color: #fff;
  font-weight: 600;
}

.preview-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  margin-right: 8px;
}

.preview-title {
  font-size: 14px;
}

.preview-body {
  padding: 12px;
  min-height: 80px;
}

.preview-bubble {
  background: #f5f5f5;
  border-left: 3px solid #1890ff;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 13px;
  color: #333;
  line-height: 1.5;
}

.preview-input {
  padding: 8px 12px 12px;
}
</style>
