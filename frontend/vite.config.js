import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  devServer: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080/api',
        changeOrigin: true, // cross origin 허용
        pathRewrite: {
          '^/api': ''
        }
      },
    }
  },
})
