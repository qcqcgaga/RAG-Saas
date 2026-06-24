import { defineConfig } from 'vite'

export default defineConfig({
  build: {
    lib: {
      entry: 'src/main.ts',
      name: 'DocChatWidget',
      formats: ['iife'],
      fileName: () => 'widget.js',
    },
    outDir: 'dist',
    rollupOptions: {
      output: {
        extend: true,
        assetFileNames: 'widget.[ext]',
      },
    },
  },
})
