<!-- src/components/MessageBox/FinalConfirm.vue -->
<template>
  <div class="final-confirm-container">
    <div class="icon-wrapper">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"></path>
      </svg>
    </div>
    <div class="content-wrapper">
      <h3>마지막 확인</h3>
      <p>계정 정보는 영구적으로 삭제됩니다.<br/>정말로 탈퇴하시겠습니까?</p>
    </div>
  </div>
</template>

<style scoped>
/* ------------------------------------ */
/*           컴포넌트 변수 정의            */
/* ------------------------------------ */
.final-confirm-container {
  --final-confirm-title-color: var(--el-text-color-primary, #303133);
  --final-confirm-text-color: var(--el-text-color-regular, #606266);
  --final-confirm-icon-color: var(--el-color-warning, #e6a23c);
}

:deep(html.dark) .final-confirm-container {
  --final-confirm-title-color: #EAECEF;
  --final-confirm-text-color: #c0c4cc;
  --final-confirm-icon-color: #e6a23c;
}

/* ------------------------------------ */
/*           실제 스타일 적용             */
/* ------------------------------------ */

.final-confirm-container {
  display: flex;
  align-items: flex-start;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  padding: 5px;
}

.icon-wrapper {
  margin-right: 15px;
  color: var(--final-confirm-icon-color);
  flex-shrink: 0;
  margin-top: 2px;
}

.icon-wrapper svg {
  width: 28px;
  height: 28px;
}

.content-wrapper h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--final-confirm-title-color);
  margin-top: 0;
  margin-bottom: 8px;
}

.content-wrapper p {
  font-size: 15px;
  color: var(--final-confirm-text-color);
  line-height: 1.6;
  margin: 0;
}
</style>