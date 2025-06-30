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
  /* 인라인 스타일을 클래스로 옮겼습니다. */
  position: fixed;
  bottom: 2%;
  left: 0;
  right: 0;

  /* 스타일 개선 */
  font-size: 12px;
  text-align: center;
  color: var(--el-text-color-secondary); /* 테마와 어울리는 부드러운 색상 사용 */
  z-index: 1000; /* 다른 요소에 가려지지 않도록 z-index 설정 */
  pointer-events: none; /* 푸터가 다른 클릭 가능한 요소를 가리는 것을 방지 */
}

.app-footer strong {
  font-weight: 600; /* 좀 더 명확한 굵기 지정 */
  color: var(--el-text-color-regular);
}
</style>