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
          <div class="preview-header-bar">
            <h4>组件预览</h4>
            <a-space>
              <a-button size="small" @click="handlePreviewReset">
                <template #icon><ReloadOutlined /></template>
                重置对话
              </a-button>
            </a-space>
          </div>
          <div class="preview-container" v-if="widgetToken">
            <iframe
              ref="previewIframe"
              :src="previewSrc"
              class="preview-iframe"
            ></iframe>
          </div>
          <div class="preview-container" v-else>
            <a-empty description="保存配置后可预览" />
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import type { WidgetConfig } from '@/types/api'
import { getWidgetConfig, updateWidgetConfig, getEmbedScript, regenerateToken } from '@/api/widget'

const form = reactive<WidgetConfig>({
  brandColor: '#1890ff',
  welcomeMessage: '您好！有什么可以帮您？',
  iconUrl: null,
  enabled: true,
})

const saving = ref(false)
const embedScript = ref('')
const widgetToken = ref('')
const previewIframe = ref<HTMLIFrameElement | null>(null)

/** 预览 iframe 的 src URL：指向后端预览页面 */
const previewSrc = computed(() => {
  if (!widgetToken.value) return ''
  const apiBaseUrl = import.meta.env.VITE_WIDGET_API_URL
    || (import.meta.env.DEV ? `http://localhost:${location.port}` : '')
    || location.origin
  return `${apiBaseUrl}/widget-preview.html?token=${encodeURIComponent(widgetToken.value)}&apiUrl=${encodeURIComponent(apiBaseUrl)}`
})

async function fetchConfig() {
  try {
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
    // 从嵌入脚本中提取 token
    const match = embedScript.value.match(/data-api-key="([^"]+)"/)
    if (match && match[1]) {
      widgetToken.value = match[1]
      localStorage.setItem('widgetToken', match[1])
    }
  } catch { /* handled */ }
}

async function handleSave() {
  saving.value = true
  try {
    const res = await updateWidgetConfig({ ...form })
    Object.assign(form, res.data)
    message.success('配置已保存')
    // 通知预览 iframe 更新配置
    sendConfigUpdate()
    // 刷新嵌入脚本
    fetchEmbedScript()
  } catch { /* handled */ } finally {
    saving.value = false
  }
}

/** 向预览 iframe 发送配置更新消息 */
function sendConfigUpdate() {
  if (previewIframe.value?.contentWindow) {
    previewIframe.value.contentWindow.postMessage({
      type: 'config-update',
      source: 'docchat-admin',
      config: {
        brandColor: form.brandColor,
        welcomeMessage: form.welcomeMessage,
        iconUrl: form.iconUrl,
      }
    }, '*')
  }
}

/** 重置预览窗口中的对话 */
function handlePreviewReset() {
  if (previewIframe.value?.contentWindow) {
    previewIframe.value.contentWindow.postMessage({
      type: 'reset',
      source: 'docchat-admin',
    }, '*')
    message.success('对话已重置')
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

.preview-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.preview-header-bar h4 {
  margin: 0;
}

.preview-container {
  display: flex;
  justify-content: center;
  padding: 16px 0;
}

.preview-iframe {
  width: 380px;
  height: 500px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background: #fff;
}
</style>
