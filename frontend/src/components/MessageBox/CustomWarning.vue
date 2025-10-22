<template>
  <div class="custom-warning-container">
    <!-- 왼쪽 아이콘 영역 -->
    <div class="icon-wrapper">
      <el-icon :size="32">
        <WarnTriangleFilled />
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
// 아이콘을 @element-plus/icons-vue에서 가져옵니다.
import { WarnTriangleFilled } from '@element-plus/icons-vue';

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
/* 애니메이션 정의 (기존과 동일) */
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
.custom-warning-container {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 24px;
  background-color: var(--el-fill-color-lighter);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
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
  border-radius: 50%;
  background-color: var(--el-color-danger-light-9);
  animation: icon-pop-in 0.5s ease-out 0.1s backwards;
}

/* 아이콘 색상 변경 */
.icon-wrapper .el-icon {
  /* 아이콘 색상을 warning 계열로 변경 */
  color: var(--el-color-danger);
}

/* 텍스트 콘텐츠 영역 (기존과 동일) */
.content-wrapper {
  text-align: left;
  animation: text-slide-up 0.5s ease-out 0.2s backwards;
}

.title {
  font-size: 19px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin: 0 0 8px 0;
}

.message {
  font-size: 15px;
  color: var(--el-text-color-secondary);
  margin: 0;
  line-height: 1.6;
}
</style>