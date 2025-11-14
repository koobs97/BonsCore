<script setup lang="ts">
import { defineProps } from 'vue';
import { WarningFilled } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

defineProps<{
  title: string;
  message: string;
}>();
</script>

<template>
  <div class="custom-confirm-container">
    <!-- 1. 아이콘 -->
    <div class="icon-wrapper">
      <el-icon :size="32">
        <WarningFilled />
      </el-icon>
    </div>

    <!-- 2. 텍스트 영역 -->
    <div class="text-wrapper">
      <h3 class="custom-title">{{ title }}</h3>
      <p class="custom-message" v-html="message"></p>
      <p class="custom-guide">
        {{ t('login.dialogs.duplicateLogin.guide') }}
      </p>
    </div>
  </div>
</template>

<style scoped>
@keyframes container-fade-in {
  from {
    opacity: 0;
    transform: scale(0.97);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
@keyframes icon-pop-in {
  0% {
    transform: scale(0.5);
    opacity: 0;
  }
  60% {
    transform: scale(1.1);
    opacity: 1;
  }
  100% {
    transform: scale(1);
  }
}
@keyframes text-slide-up {
  from {
    opacity: 0;
    transform: translateY(15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.custom-confirm-container {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 16px 12px 12px 12px;
  background-color: var(--el-border-color-extra-light);
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
  animation: container-fade-in 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}
.icon-wrapper {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background-color: var(--el-color-warning-light-9);
  border: 1px solid var(--el-border-color);
  animation: icon-pop-in 0.5s cubic-bezier(0.68, -0.55, 0.27, 1.55) 0.1s backwards;
}
.icon-wrapper .el-icon {
  color: var(--el-color-warning);
}
.text-wrapper {
  text-align: left;
  padding-top: 4px;
  animation: text-slide-up 0.5s ease-out 0.2s backwards;
}
.custom-title {
  font-size: 19px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 0 0 10px 0;
}
.custom-message {
  font-size: 15px;
  color: var(--el-text-color-regular);
  line-height: 1.7;
  margin: 0 0 16px 0;
}
.custom-guide {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  background-color: var(--el-fill-color-light);
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-extra-light);
  margin: 0;
  line-height: 1.6;
}
</style>
<style>
.el-message-box.custom-message-box-ko {
  width: 490px !important;
  max-width: none;
}
.el-message-box.custom-message-box-en {
  width: 533px !important;
  max-width: none;
}
.el-message-box__header {
  padding: 0 !important;
}
</style>