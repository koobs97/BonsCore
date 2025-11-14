<script setup lang="ts">
/**
 * ========================================
 * 파일명   : ServiceErrorState.vue
 * ----------------------------------------
 * 설명     : 서비스 에러시 화면 송출용 컴포넌트
 * 작성자   : koobonsang
 * 버전     : 1.0
 * 작성일자 : 2025-11-14
 * ========================================
 */
import { Refresh } from '@element-plus/icons-vue';

defineProps({
  title: { type: String, required: true },
  message: { type: String, required: true },
  showFallbackAction: {
    type: Boolean,
    default: false,
  },
  fallbackActionText: {
    type: String,
    default: '대안 보기',
  },
  retryActionText: {
    type: String,
    default: '다시 시도',
  }
});

const emit = defineEmits(['retry', 'fallback']);
</script>

<template>
  <div class="error-state-container">
    <div class="icon-wrapper">
      <svg width="26" height="26" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M17.3413 6.94678L6.65851 17.6296" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M14.5011 3.55322C11.954 2.58965 9.00007 2.85215 6.63412 4.39032C4.26817 5.92849 2.76864 8.44131 2.59902 11.2359C2.4294 14.0305 3.61668 16.7362 5.76562 18.361C7.91457 19.9858 10.7687 20.263 13.3158 19.0988" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M9.49814 20.4467C12.0452 21.4103 14.9992 21.1478 17.3651 19.6096C19.7311 18.0715 21.2306 15.5587 21.4002 12.7641C21.5698 9.96946 20.3825 7.26383 18.2336 5.63902C16.0847 4.01422 13.2305 3.737 10.6834 4.90118" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </div>
    <h4 class="error-title">{{ title }}</h4>
    <p class="error-message">{{ message }}</p>

    <div class="button-group">
      <!-- v-if를 사용하여 대안 액션 버튼을 조건부로 렌더링 -->
      <el-button
          size="small"
          v-if="showFallbackAction"
          @click="emit('fallback')"
          class="fallback-button"
      >
        {{ fallbackActionText }}
      </el-button>
      <el-button
          :icon="Refresh"
          plain
          size="small"
          @click="emit('retry')"
          class="retry-button"
      >
        {{ retryActionText }}
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.error-state-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 12px;
  height: 100%;
  background-color: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
}
.icon-wrapper {
  margin-bottom: 4px;
  color: var(--el-text-color-placeholder);
}
.error-title {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 8px 0;
}
.error-message {
  font-size: 0.7rem;
  color: var(--el-text-color-secondary);
  margin: 0 0 4px 0;
  line-height: 1.6;
  max-width: 320px; /* 메시지가 너무 길어지지 않도록 */
}
.retry-button {
  font-size: 0.6rem;
  font-weight: 500;
  background-color: var(--el-color-primary);
  color: var(--el-bg-color);
}
.button-group {
  display: flex;
  gap: 2px;
  margin-top: 4px;
}
.fallback-button {
  font-size: 0.6rem;
  font-weight: 500;
}
</style>