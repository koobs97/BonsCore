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
      <el-icon :size="48" color="#E6A23C">
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
.custom-confirm-container {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 2px;
}

.icon-wrapper {
  flex-shrink: 0;
}

.text-wrapper {
  text-align: left;
}

.custom-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 8px 0;
}

.custom-message {
  font-size: 14px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
  margin: 16px 0 12px 0;
}

.custom-guide {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background-color: var(--el-bg-color-page);
  padding: 8px 12px;
  border-radius: 6px;
  margin: 0;
}

</style>