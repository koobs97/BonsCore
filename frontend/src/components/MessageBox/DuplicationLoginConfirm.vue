<script setup lang="ts">
import { defineProps } from 'vue';
import { WarningFilled } from '@element-plus/icons-vue'; // Element Plus 아이콘 사용

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
      <!-- v-html을 사용하여 서버에서 받은 <br> 태그 등을 렌더링 -->
      <p class="custom-message" v-html="message"></p>
      <p class="custom-guide">
        '로그인'을 선택하면 다른 기기에서의 접속이 종료됩니다.
      </p>
    </div>
  </div>
</template>

<style scoped>
/* 애니메이션 정의 */
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

/* 전체 컨테이너 스타일 */
.custom-confirm-container {
  display: flex;
  align-items: flex-start; /* 아이콘이 길어질 경우를 대비해 시작점으로 정렬 */
  gap: 20px;
  padding: 16px 12px 12px 12px;
  background-color: var(--el-bg-color-overlay); /* 배경색을 살짝 어둡게 하여 컨텐츠 강조 */
  border-radius: 8px; /* 모서리를 더 둥글게 */
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05); /* 그림자를 더 부드럽고 넓게 */
  animation: container-fade-in 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}

/* 아이콘 래퍼 스타일 */
.icon-wrapper {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background-color: var(--el-color-warning-light-9); /* Element Plus 경고 색상의 연한 버전 */
  animation: icon-pop-in 0.5s cubic-bezier(0.68, -0.55, 0.27, 1.55) 0.1s backwards;
}

/* 아이콘 색상 */
.icon-wrapper .el-icon {
  color: var(--el-color-warning); /* Element Plus 경고 색상 */
}

/* 텍스트 영역 */
.text-wrapper {
  text-align: left;
  padding-top: 4px; /* 아이콘과 텍스트의 시각적 높이를 맞추기 위한 미세 조정 */
  animation: text-slide-up 0.5s ease-out 0.2s backwards;
}

.custom-title {
  font-size: 19px;
  font-weight: 700; /* 폰트를 더 굵게 하여 강조 */
  color: var(--el-text-color-primary);
  margin: 0 0 10px 0;
}

.custom-message {
  font-size: 15px;
  color: var(--el-text-color-regular);
  line-height: 1.7; /* 줄 간격을 넓혀 가독성 향상 */
  margin: 0 0 16px 0;
}

.custom-guide {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  background-color: var(--el-fill-color-light); /* 배경색 변경으로 구분감 부여 */
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-extra-light);
  margin: 0;
  line-height: 1.6;
}
</style>
<style>
.el-message-box.custom-message-box {
  width: 488px !important;
  max-width: none;
}
</style>