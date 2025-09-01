<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { userStore } from "./store/userStore";

const userStoreObj = userStore();
const router = useRouter();

/**
 * zoom 대신 transform: scale()을 사용하여 화면 크기를 조정합니다.
 */
const adjustScale = () => {
  const baseWidth = 1920; // 기준 너비
  const baseHeight = 945;  // 기준 높이

  const currentWidth = window.innerWidth;
  const currentHeight = window.innerHeight;

  const scaleX = currentWidth / baseWidth;
  const scaleY = currentHeight / baseHeight;
  const scale = Math.min(scaleX, scaleY);

  // #app 요소에 transform 스타일을 적용합니다.
  const appElement = document.getElementById('app');
  if (appElement) {
    appElement.style.transform = `scale(${scale})`;
  }

  document.documentElement.style.setProperty('--app-scale', scale.toString());

  // --- ⬇️ 푸터에도 동일한 스케일을 적용하는 코드 추가 ⬇️ ---
  const footerContainer = document.getElementById('footer-container');
  if (footerContainer) {
    footerContainer.style.transform = `scale(${scale})`;
  }
  // --------------------------------------------------------
};

onMounted(() => {
  adjustScale();
  // window.addEventListener('resize', adjustScale);
});

onUnmounted(() => {
  window.removeEventListener('resize', adjustScale);

  const appElement = document.getElementById('app');
  if (appElement) {
    appElement.style.transform = 'scale(1)';
  }

  // --- ⬇️ 컴포넌트 unmount 시 푸터의 스케일도 초기화 ⬇️ ---
  const footerContainer = document.getElementById('footer-container');
  if (footerContainer) {
    footerContainer.style.transform = 'scale(1)';
  }

  document.documentElement.style.removeProperty('--app-scale');
  // ---------------------------------------------------------
});


</script>

<template>
  <router-view></router-view>
</template>

<style>
/* body 스타일은 그대로 유지해도 좋습니다. */
body {
  margin: 0;
  width: 100vw;
  height: 100vh;
  display: grid;
  place-items: center;
  overflow: hidden;
  background-color: var(--el-bg-color);
}

/* #app 스타일을 transform에 맞게 수정합니다. */
#app {

  /* scale 변환의 기준점을 중앙으로 설정합니다. */
  transform-origin: center center;

  /* 부드러운 전환 효과를 줍니다. */
  transition: transform 0.2s ease-out;

  /* 기존의 다른 스타일들 */
  overflow: auto;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.app-footer {
  position: fixed;
  bottom: 2%;
  left: 0;
  right: 0;

  font-size: 12px;
  text-align: center;
  color: var(--el-text-color-secondary);
  z-index: 1000;
  pointer-events: none;

  /* --- ⬇️ 스케일 변환의 기준점을 설정합니다 ⬇️ --- */
  /* 중앙 하단을 기준으로 크기가 조절되도록 설정 */
  transform-origin: center bottom;
  /* --------------------------------------------- */
}

.el-dialog {
  /*
    --app-scale 변수를 읽어서 transform 속성에 적용합니다.
    var() 함수의 두 번째 인자는 변수가 없을 때 사용할 기본값입니다.
  */
  transform: scale(var(--app-scale, 1));
  transform-origin: center center;
  transition: transform 0.2s ease-out; /* 스케일 변경 시 부드러운 효과 */
}
</style>