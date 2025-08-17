<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { userStore } from "./store/userStore";

const userStoreObj = userStore();
const router = useRouter();

/**
 * 창 크기가 변경될 때마다 #app 요소의 zoom 속성을 다시 계산하고 적용합니다.
 * 이 함수는 가장 간단하고 직접적인 방법입니다.
 */
const adjustZoom = () => {
  const baseWidth = 1920; // 기준 너비
  const baseHeight = 945;  // 기준 높이

  const currentWidth = window.innerWidth;
  const currentHeight = window.innerHeight;

  // 너비와 높이 비율을 계산하여 더 작은 값을 zoom 레벨로 사용합니다.
  const scaleX = currentWidth / baseWidth;
  const scaleY = currentHeight / baseHeight;
  const zoomLevel = Math.min(scaleX, scaleY);

  const appElement = document.getElementById('app');
  if (appElement) {
    // #app 요소에 직접 zoom 스타일을 적용합니다.
    appElement.style.zoom = `${zoomLevel}`;
  }
};

onMounted(() => {
  if (!userStoreObj.isLoggedIn) {
    router.push('/login');
  } else {
    router.push('/');
  }

  // 페이지가 처음 로드될 때와 창 크기가 바뀔 때마다 함수를 실행합니다.
  adjustZoom();
  window.addEventListener('resize', adjustZoom);
});

onUnmounted(() => {
  // 컴포넌트가 사라질 때 이벤트 리스너를 제거하고 zoom을 초기화합니다.
  window.removeEventListener('resize', adjustZoom);
  const appElement = document.getElementById('app');
  if (appElement) {
    appElement.style.zoom = '1';
  }
});

</script>

<template>
  <router-view></router-view>
</template>

<style scoped>

</style>
<style>
body {
  margin: 0;
  width: 100vw;
  height: 100vh;
  display: grid;
  place-items: center;
  overflow: hidden;
  background-color: var(--el-bg-color); /* 배경색은 body에 유지 */
}
#app {

  transform-origin: center center;
  overflow: auto;
  box-sizing: border-box;
  transition: transform 0.2s ease-out;

  /* --- ⬇️ 이 부분이 추가/수정되었습니다 ⬇️ --- */
  /*
    #app 스스로가 Flex 컨테이너가 되어,
    내부의 컨텐츠(<router-view>)를 정중앙에 배치합니다.
  */
  display: flex;
  flex-direction: column; /* 컨텐츠를 세로로 쌓음 */
  align-items: center;     /* 수평 중앙 정렬 */
}
</style>
