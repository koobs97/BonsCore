<script setup lang="ts">
import { SwitchButton } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';
const { t } = useI18n();
</script>

<template>
  <div class="logout-confirm-container">
    <!-- 왼쪽 아이콘 영역 -->
    <div class="icon-wrapper">
      <el-icon :size="32">
        <SwitchButton />
      </el-icon>
    </div>

    <!-- 오른쪽 텍스트 콘텐츠 영역 -->
    <div class="content-wrapper">
      <h2 class="title">{{ t('logout.dialog.title') }}</h2>
      <p class="message">{{ t('logout.dialog.message') }}</p>
    </div>
  </div>
</template>

<style scoped>
/* 애니메이션 정의 (참고 스타일에서 가져옴) */
@keyframes container-fade-in {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes icon-pop-in {
  0% {
    transform: scale(0.6);
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
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 전체 컨테이너 스타일 */
.logout-confirm-container {
  display: flex;
  align-items: center;
  gap: 24px; /* 아이콘과 텍스트 사이 간격 */
  padding: 24px; /* 내부 여백 증가로 여유 공간 확보 */
  background-color: var(--el-border-color-extra-light);
  border-radius: 8px; /* 부드러운 모서리 */
  border: 1px solid var(--el-border-color);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); /* 입체감을 위한 그림자 */
  animation: container-fade-in 0.4s ease-out forwards;
}

/* 아이콘 래퍼 스타일 */
.icon-wrapper {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  border-radius: 50%; /* 완벽한 원형 */
  /* 로그아웃 경고에 맞게 'danger' 색상 사용 */
  background-color: var(--el-color-danger-light-9);
  animation: icon-pop-in 0.5s ease-out 0.1s backwards;
  border: 1px solid var(--el-color-danger-light-8);
}

/* 아이콘 색상 변경 */
.icon-wrapper .el-icon {
  color: var(--el-color-danger); /* Element Plus 위험 색상 */
}

/* 텍스트 콘텐츠 영역 */
.content-wrapper {
  text-align: left;
  animation: text-slide-up 0.5s ease-out 0.2s backwards;
}

.title {
  font-size: 19px; /* 제목 폰트 크기 증가 */
  font-weight: 700; /* 제목 폰트 굵게 */
  color: var(--el-text-color-primary);
  margin: 0 0 8px 0;
}

.message {
  font-size: 15px; /* 본문 폰트 크기 증가 */
  color: var(--el-text-color-secondary); /* 본문은 약간 연한 색으로 */
  margin: 0;
  line-height: 1.6; /* 줄 간격을 넓혀 가독성 향상 */
}
</style>
<style>
.el-button--primary {
  color: var(--el-bg-color) !important;
}
.el-message-box {
  width: 470px;
  max-width: 470px;
}
</style>