import { defineConfig } from 'vite'
import { resolve } from 'node:path'

/**
 * 聊天组件 Vite 配置
 *
 * 输出 IIFE 格式，可直接嵌入任意网页：
 * <script src="https://cdn.example.com/docchat-widget.js"></script>
 * <script>DocChatWidget.init({ apiKey: 'xxx' })</script>
 */
export default defineConfig({
  build: {
    lib: {
      entry: resolve(__dirname, 'src/main.ts'),
      name: 'DocChatWidget',
      fileName: 'docchat-widget',
      formats: ['iife'],
    },
    outDir: 'dist',
    rollupOptions: {
      output: {
        assetFileNames: 'docchat-widget.[ext]',
      },
    },
  },
})
