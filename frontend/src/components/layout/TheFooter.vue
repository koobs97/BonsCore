<template>
  <footer class="app-footer">
    <strong>Copyright</strong> KooBonSang © {{ currentYear }} All Rights Reserved.
  </footer>
</template>

<script setup>
import { computed } from 'vue';

// 매번 수정할 필요 없이 현재 연도를 자동으로 가져옵니다.
const currentYear = computed(() => new Date().getFullYear());
</script>

<style scoped>
.app-footer {
  bottom: 2%;
  left: 0;
  right: 0;
  font-size: 12px;
  text-align: center;
  color: var(--el-text-color-secondary);
  z-index: 1000;
  pointer-events: none;
  transform-origin: center bottom;
}

.app-footer strong {
  font-weight: 600;
  color: var(--el-text-color-regular);
}
</style>