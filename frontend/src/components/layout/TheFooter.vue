<template>
  <footer class="app-footer">
    <el-text>© {{ currentYear }} Koo Bon Sang. All rights reserved.</el-text>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue';

// 매번 수정할 필요 없이 현재 연도를 자동으로 가져옵니다.
const currentYear = computed(() => new Date().getFullYear());
</script>

<style scoped>
.app-footer {
  position: fixed;
  z-index: 1000;
  pointer-events: none;
  --app-scale: 1;
  width: 90vw;
  left: -45vw;
  text-align: center;
  transform-origin: center bottom;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.app-footer el-text {
  font-weight: 500;
  color: var(--el-text-color-regular);
}
</style>