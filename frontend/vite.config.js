import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path';

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
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
  },
})
