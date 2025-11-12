<script setup lang="ts">
import { useI18n } from "vue-i18n";

const { t } = useI18n();
</script>
<template>
  <div class="final-confirm-container">
    <div class="icon-wrapper">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"></path>
      </svg>
    </div>
    <div class="content-wrapper">
      <h3>{{ t('withdrawConfirm.finalConfirmDialog.title') }}</h3>
      <p>
        {{ t('withdrawConfirm.finalConfirmDialog.line1') }}
        <br/>
        {{ t('withdrawConfirm.finalConfirmDialog.line2') }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.final-confirm-container {
  --final-confirm-background: var(--el-fill-color-lighter);
  --final-confirm-title-color: var(--el-text-color-primary);
  --final-confirm-text-color: var(--el-text-color-placeholder);
  --final-confirm-icon-color: #f59e0b;
  --final-confirm-icon-background: var(--el-bg-color-overlay);
  --final-confirm-border-radius: 12px;
  --final-confirm-box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  --final-confirm-padding: 24px;
}
.final-confirm-container {
  display: flex;
  align-items: flex-start;
  font-family: 'Pretendard', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  padding: var(--final-confirm-padding);
  background-color: var(--final-confirm-background);
  border-radius: var(--final-confirm-border-radius);
  box-shadow: var(--final-confirm-box-shadow);
  transition: all 0.3s ease-in-out;
  border: 1px solid rgba(0,0,0,0.05);
}
.final-confirm-container:hover {
  transform: translateY(-3px);
  box-shadow: 0 15px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
}
.icon-wrapper {
  margin-right: 20px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: var(--final-confirm-icon-background);
  color: var(--final-confirm-icon-color);
  animation: pulse-icon 2s infinite;
}

.icon-wrapper svg {
  width: 28px;
  height: 28px;
}

.content-wrapper h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--final-confirm-title-color);
  margin: 0 0 8px;
}

.content-wrapper p {
  font-size: 15px;
  color: var(--final-confirm-text-color);
  line-height: 1.6;
  margin: 0;
}

@keyframes pulse-icon {
  0% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.4);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 10px rgba(245, 158, 11, 0);
  }
  100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(245, 158, 11, 0);
  }
}
</style>