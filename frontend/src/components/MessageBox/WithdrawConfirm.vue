<script setup lang="ts">
/**
 * ========================================
 * 파일명   : WithdrawConfirm.vue
 * ----------------------------------------
 * 설명     : 회원탈퇴 다이얼로그
 * 작성자   : koobonsang
 * 버전     : 1.0
 * 작성일자 : 2025-10-05
 * ========================================
 */
import { ref } from 'vue';
import { useI18n } from "vue-i18n";

const { t } = useI18n();
const confirmText = ref('');
const emit = defineEmits(['update:text']);

// 이벤트가 발생할 때마다 부모에게 값을 전달하는 함수
const onInput = (value: string) => {
  emit('update:text', value);
}
</script>
<template>
  <div class="withdraw-container">
    <h3>{{ t('withdrawConfirm.title') }}</h3>
    <p class="subtitle">{{ t('withdrawConfirm.subtitle') }}</p>

    <div class="warning-box">
      <div class="icon-wrapper">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"></path>
        </svg>
      </div>
      <div class="content-wrapper">
        <p>{{ t('withdrawConfirm.warning.message') }}</p>
        <ul>
          <li>{{ t('withdrawConfirm.warning.item1') }}</li>
          <li>{{ t('withdrawConfirm.warning.item2') }}</li>
        </ul>
      </div>
    </div>

    <p class="final-confirm-text">
      {{ t('withdrawConfirm.finalConfirm.line1') }}<br/>
      <i18n-t keypath="withdrawConfirm.finalConfirm.line2" tag="span">
        <template #actionText>
          <b>{{ t('withdrawConfirm.finalConfirm.actionText') }}</b>
        </template>
      </i18n-t>
    </p>

    <!--
      @paste.prevent : 복사 붙여넣기 방지
      @drop.prevent : 드래그 앤 드롭 방지
    -->
    <el-input
        v-model="confirmText"
        @input="onInput"
        :placeholder="t('withdrawConfirm.placeholder', { actionText: t('withdrawConfirm.finalConfirm.actionText') })"
        size="large"
        @paste.prevent
        @drop.prevent
    />
  </div>
</template>
<style scoped>
.withdraw-container {
  --withdraw-title-color: var(--el-text-color-primary, #2c3e50);
  --withdraw-subtitle-color: var(--el-text-color-secondary, #8492a6);
  --withdraw-text-color: var(--el-text-color-regular, #5a687c);

  --withdraw-warning-bg: var(--el-color-danger-light-9, #fef0f0);
  --withdraw-warning-border: var(--el-color-danger, #f56c6c);
  --withdraw-warning-icon-color: var(--el-color-danger, #f56c6c);
  --withdraw-warning-title-color: var(--el-color-danger-dark-2, #d33a3a);
  --withdraw-warning-list-color: var(--el-color-danger-light-3, #e57474);
}

/* html에 .dark 클래스가 있을 때 적용될 색상 변수 */
/* :deep()을 사용하여 상위 html 태그를 참조합니다. */
:deep(html.dark) .withdraw-container {
  --withdraw-title-color: #EAECEF;
  --withdraw-subtitle-color: #a8abb2;
  --withdraw-text-color: #c0c4cc;

  --withdraw-warning-bg: rgba(245, 108, 108, 0.1); /* 어두운 배경에 맞는 붉은 톤 */
  --withdraw-warning-border: #f56c6c;
  --withdraw-warning-icon-color: #f56c6c;
  --withdraw-warning-title-color: #f89898;
  --withdraw-warning-list-color: #fab6b6;
}


/* ----------------------------------------- */
/* 2. 변수를 사용한 실제 스타일 적용           */
/* ----------------------------------------- */

.withdraw-container {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  text-align: center;
  padding: 0;
}

/* 메인 타이틀 */
h3 {
  font-size: 24px;
  font-weight: 700;
  color: var(--withdraw-title-color);
  margin-top: 0;
  margin-bottom: 8px;
}

/* 서브 타이틀 */
.subtitle {
  font-size: 16px;
  color: var(--withdraw-subtitle-color);
  margin-bottom: 24px;
}

/* --- 경고 박스 디자인 --- */
.warning-box {
  display: flex;
  align-items: flex-start;
  background-color: var(--withdraw-warning-bg);
  border-left: 5px solid var(--withdraw-warning-border);
  border-radius: 4px;
  padding: 16px;
  margin-bottom: 28px;
  text-align: left;
  transition: background-color 0.3s, border-color 0.3s; /* 부드러운 전환 효과 */
}

/* 아이콘을 감싸는 래퍼 */
.icon-wrapper {
  margin-right: 12px;
  flex-shrink: 0;
  color: var(--withdraw-warning-icon-color);
  transition: color 0.3s;
}

.icon-wrapper svg {
  width: 24px;
  height: 24px;
  position: relative;
  top: -1px;
}

/* 텍스트 컨텐츠를 감싸는 래퍼 */
.content-wrapper {
  flex-grow: 1;
}

.content-wrapper p {
  margin: 0 0 12px 0;
  font-weight: 600;
  color: var(--withdraw-warning-title-color);
  font-size: 15px;
  line-height: 1.5;
  transition: color 0.3s;
}

.content-wrapper ul {
  list-style-type: none;
  padding-left: 0;
  margin-top: 8px;
  margin-bottom: 0;
  color: var(--withdraw-warning-list-color);
  font-size: 14px;
  transition: color 0.3s;
}

.content-wrapper ul li {
  margin-bottom: 2px;
  line-height: 1.6;
}

/* 최종 확인 텍스트 */
.final-confirm-text {
  font-size: 15px;
  color: var(--withdraw-text-color);
  margin-bottom: 12px;
  line-height: 1.6;
  transition: color 0.3s;
}

.final-confirm-box .el-message-box__header {
  display: none;
}
.final-confirm-box .el-message-box__content {
  padding: 0;
}
/* 최종 확인 텍스트의 굵은 글씨 스타일 */
.final-confirm-text b {
  color: var(--withdraw-warning-border);
  font-weight: 700;
}
.el-input {
  margin-top: 0;
}
</style>