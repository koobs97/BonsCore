<script setup lang="ts">
import { defineEmits } from 'vue';
import { WarningFilled } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

// 부모 컴포넌트로 이벤트를 전달하기 위한 emit 정의
const emit = defineEmits(['go-to-login', 'go-to-change-password']);

const handleLoginClick = () => {
  emit('go-to-login');
};

const handleChangePasswordClick = () => {
  emit('go-to-change-password');
};
</script>

<template>
  <div class="duplicate-login-container">
    <!-- 1. 아이콘 -->
    <div class="icon-wrapper">
      <el-icon :size="32">
        <WarningFilled />
      </el-icon>
    </div>

    <!-- 2. 텍스트 영역 -->
    <div class="text-wrapper">
      <h3 class="custom-title">{{ t('session.duplicateLogin.title') }}</h3>
      <p class="custom-message">{{ t('session.duplicateLogin.message') }}</p>
      <p class="custom-guide">
        {{ t('session.duplicateLogin.securityWarning') }}
      </p>
    </div>
  </div>

  <!-- 3. 액션 버튼 영역 -->
  <div class="action-buttons">
    <el-button @click="handleLoginClick">
      {{ t('session.duplicateLogin.buttonLogin') }}
    </el-button>
    <el-button type="primary" @click="handleChangePasswordClick">
      {{ t('session.duplicateLogin.buttonChangePassword') }}
    </el-button>
  </div>
</template>

<style scoped>
.duplicate-login-container {
  display: flex;
  align-items: flex-start;
  gap: 20px;
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
}
.icon-wrapper .el-icon {
  color: var(--el-color-warning);
}
.text-wrapper {
  text-align: left;
  padding-top: 4px;
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
  color: var(--el-color-warning-dark-2); /* 강조를 위해 색상 변경 */
  background-color: var(--el-color-warning-light-9);
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid var(--el-color-warning-light-7);
  margin: 0;
  line-height: 1.6;
}
.action-buttons {
  display: flex;
  justify-content: flex-end;
  margin-top: 25px;
  padding-top: 10px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
<style>
.duplicate-login-box-ko {
  --el-messagebox-width: 472px !important;
  border-radius: 8px;
}
.duplicate-login-box-en {
  --el-messagebox-width: 472px !important;
  border-radius: 8px;
}
.duplicate-login-box-ko .el-message-box__btns {
  padding: 0 !important;
}
.duplicate-login-box-en .el-message-box__btns {
  padding: 0 !important;
}
</style>