import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path';

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080/api',
        changeOrigin: true, // cross origin 허용
        rewrite: (path) => path.replace(/^\/api/, '')
      },
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"), // @를 src 폴더로 매핑
    },
  },
})
