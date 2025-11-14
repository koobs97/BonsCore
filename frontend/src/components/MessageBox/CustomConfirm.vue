<template>
  <div class="custom-confirm-container">
    <!-- 왼쪽 아이콘 영역 -->
    <div class="icon-wrapper">
      <el-icon :size="32">
        <CircleCheckFilled />
      </el-icon>
    </div>

    <!-- 오른쪽 텍스트 콘텐츠 영역 -->
    <div class="content-wrapper">
      <h2 class="title">{{ title }}</h2>
      <p class="message">{{ message }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CircleCheckFilled } from '@element-plus/icons-vue';

// 부모로부터 제목과 메시지를 props로 받습니다.
defineProps({
  title: {
    type: String,
    required: true,
  },
  message: {
    type: String,
    required: true,
  },
});
</script>

<style scoped>
/* 애니메이션 정의 */
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
.custom-confirm-container {
  display: flex;
  align-items: center;
  gap: 24px; /* 아이콘과 텍스트 사이 간격 증가 */
  padding: 24px; /* 내부 여백 증가 */
  background-color: var(--el-border-color-extra-light);
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); /* 부드러운 그림자 효과 */
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
  border-radius: 50%; /* 완벽한 원 */
  background-color: var(--el-color-success-light-8); /* Element Plus 성공 색상의 연한 버전 */
  animation: icon-pop-in 0.5s ease-out 0.1s backwards; /* 0.1초 뒤에 시작 */
}

/* 아이콘 색상 변경 */
.icon-wrapper .el-icon {
  color: var(--el-color-success); /* Element Plus 성공 색상 */
}

/* 텍스트 콘텐츠 영역 */
.content-wrapper {
  text-align: left;
  animation: text-slide-up 0.5s ease-out 0.2s backwards;
}

.title {
  font-size: 19px; /* 폰트 크기 증가 */
  font-weight: 700; /* 폰트 굵기 증가 */
  color: var(--el-text-color-primary);
  margin: 0 0 8px 0;
}

.message {
  font-size: 15px; /* 폰트 크기 증가 */
  color: var(--el-text-color-secondary); /* 본문은 약간 연한 색으로 */
  margin: 0;
  line-height: 1.6; /* 줄 간격 조정 */
}
</style>
<style>
.el-message-box__message {
  width: 100%;
}
</style>