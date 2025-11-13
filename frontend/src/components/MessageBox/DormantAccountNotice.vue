<script setup lang="ts">
import { Lock } from "@element-plus/icons-vue";
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

defineProps({
  title: String,
  message: String,
});
</script>

<template>
  <div class="gradient-notice-container">
    <!-- 비주얼 헤더 영역 -->
    <div class="visual-header">
      <el-icon :size="60">
        <Lock />
      </el-icon>
    </div>

    <!-- 텍스트 컨텐츠 영역 -->
    <div class="text-content">
      <h3 class="confirm-title">{{ title || t('login.dialogs.dormantAccount.title') }}</h3>
      <p class="confirm-message" v-html="message || t('login.dialogs.dormantAccount.defaultMessage')"></p>
    </div>
  </div>
</template>

<style scoped>
.gradient-notice-container {
  text-align: center;
}
.visual-header {
  padding: 32px 24px;
  background: linear-gradient(135deg, #fdfcfb 0%, #e2d1c3 100%);
  border-top-left-radius: 6px;
  border-top-right-radius: 6px;
  display: flex;
  justify-content: center;
  align-items: center;
}
html.dark .visual-header {
  background: linear-gradient(135deg, #4a4a4a 0%, #2c2c2c 100%);
}
.visual-header .el-icon {
  color: #8c6a4a;
  filter: drop-shadow(0 4px 6px rgba(0, 0, 0, 0.1));
}
html.dark .visual-header .el-icon {
  color: #c8a98a;
}
.text-content {
  padding: 24px 10px;
}
.confirm-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 12px;
  color: var(--el-text-color-primary);
}
.confirm-message {
  font-size: 15px;
  line-height: 1.7;
  margin: 0;
  color: var(--el-text-color-secondary);
}
</style>
<style>
.dormant-account-box-ko {
  --el-messagebox-width: 430px !important;
  padding: 0 10px 10px 10px !important;
  border-radius: 8px;
}
.dormant-account-box-en {
  --el-messagebox-width: 470px !important;
  padding: 0 10px 10px 10px !important;
  border-radius: 8px;
}
.el-message-box__header {
  padding-bottom: 0;
}
.dormant-account-box .el-message-box__header {
  display: none;
}
.dormant-account-box .el-message-box__content {
  padding: 0;
}
.dormant-account-box .el-message-box__btns {
  padding: 16px 24px 24px;
}
</style>