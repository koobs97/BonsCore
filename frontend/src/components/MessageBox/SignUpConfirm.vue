<template>
  <div class="custom-confirm-container">
    <!-- 왼쪽 아이콘 영역 -->
    <div class="icon-wrapper">
      <el-icon :size="48" color="#409EFC">
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
.custom-confirm-container {
  display: flex;
  align-items: center;
  gap: 20px; /* 아이콘과 텍스트 사이의 간격 */
  padding: 10px 5px; /* 내부 여백 */
}

.icon-wrapper {
  /* 아이콘이 텍스트가 길어져도 찌그러지지 않도록 함 */
  flex-shrink: 0;
}

.content-wrapper {
  text-align: left;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0; /* 위/오른쪽/아래/왼쪽 마진 */
}

.message {
  font-size: 14px;
  color: #606266;
  margin: 0;
  line-height: 1.5;
}
</style>