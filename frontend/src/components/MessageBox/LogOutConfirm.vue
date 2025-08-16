<script setup lang="ts">
import { SwitchButton } from '@element-plus/icons-vue';
</script>

<template>
  <div class="logout-confirm-container-compact">
    <!-- 아이콘과 텍스트를 한 줄에 배치 -->
    <div class="icon-wrapper">
      <el-icon :size="40" color="#F56C6C">
        <SwitchButton />
      </el-icon>
    </div>
    <div class="text-wrapper">
      <h3 class="custom-title">정말 로그아웃 하시겠습니까?</h3>
      <p class="custom-message">
        저장하지 않은 데이터는 복구할 수 없습니다.
      </p>
    </div>
  </div>
</template>

<style scoped>
/* 전체 컨테이너를 Flexbox로 설정하여 좌우 배치 */
.logout-confirm-container-compact {
  display: flex;
  align-items: center; /* 세로 중앙 정렬 */
  gap: 16px; /* 아이콘과 텍스트 사이의 간격 */
  padding: 8px 4px; /* 약간의 내부 여백 */
}

.icon-wrapper {
  flex-shrink: 0; /* 아이콘이 찌그러지지 않도록 */
}

.text-wrapper {
  text-align: left; /* 텍스트는 왼쪽 정렬 */
}

.custom-title {
  font-size: 16px; /* 제목을 더 간결하게 */
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 5px 0; /* 제목과 메시지 사이 간격 줄임 */
}

.custom-message {
  font-size: 13px; /* 메시지 폰트 크기 줄임 */
  color: #909399; /* 덜 중요하므로 회색 텍스트로 */
  line-height: 1.4;
  margin: 0;
}
.el-button--primary {
  --el-button-text-color: var(--el-color-white);
  --el-button-bg-color: var(--el-color-primary);
  --el-button-border-color: var(--el-color-primary);
  --el-button-outline-color: var(--el-color-primary-light-5);
  --el-button-active-color: var(--el-color-primary-dark-2);
  --el-button-hover-text-color: var(--el-color-white);
  --el-button-hover-link-text-color: var(--el-color-primary-light-5);
  --el-button-hover-bg-color: var(--el-color-primary-light-3);
  --el-button-hover-border-color: var(--el-color-primary-light-3);
  --el-button-active-bg-color: var(--el-color-primary-dark-2);
  --el-button-active-border-color: var(--el-color-primary-dark-2);
  --el-button-disabled-text-color: var(--el-color-white);
  --el-button-disabled-bg-color: var(--el-color-primary-light-5);
  --el-button-disabled-border-color: var(--el-color-primary-light-5);
  color: var(--el-bg-color);
}
</style>
<style>

.el-button--primary {
  --el-button-text-color: var(--el-color-white);
  --el-button-bg-color: var(--el-color-primary);
  --el-button-border-color: var(--el-color-primary);
  --el-button-outline-color: var(--el-color-primary-light-5);
  --el-button-active-color: var(--el-color-primary-dark-2);
  --el-button-hover-text-color: var(--el-color-white);
  --el-button-hover-link-text-color: var(--el-color-primary-light-5);
  --el-button-hover-bg-color: var(--el-color-primary-light-3);
  --el-button-hover-border-color: var(--el-color-primary-light-3);
  --el-button-active-bg-color: var(--el-color-primary-dark-2);
  --el-button-active-border-color: var(--el-color-primary-dark-2);
  --el-button-disabled-text-color: var(--el-color-white);
  --el-button-disabled-bg-color: var(--el-color-primary-light-5);
  --el-button-disabled-border-color: var(--el-color-primary-light-5);
  color: var(--el-bg-color) !important;
}
</style>
