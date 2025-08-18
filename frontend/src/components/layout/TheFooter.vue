<template>
  <footer class="app-footer">
    <strong>Copyright</strong> KooBonSang © {{ currentYear }} All Rights Reserved.
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

  /* --- ⬇️ 여기가 핵심 수정 부분입니다 ⬇️ --- */

  /* 기본 스케일 값을 1로 설정 (JS가 로드되기 전 대비) */
  --app-scale: 1;

  /* 1. 너비를 100vw로 설정하여 항상 뷰포트 너비를 채우도록 합니다. */
  width: 90vw;
  /* left 위치를 조정하여 거대한 박스의 중앙이 화면 중앙에 오도록 합니다. */
  left: -45vw;

  /* 텍스트는 이 거대한 박스 안에서 중앙 정렬됩니다. */
  text-align: center;

  /* transform-origin은 중앙 하단으로 유지합니다. */
  transform-origin: center bottom;

  /* 5. 폰트 크기는 고정값으로 두되, 필요하다면 scale을 적용할 수 있습니다. */
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.app-footer strong {
  font-weight: 600;
  color: var(--el-text-color-regular);
}
</style>