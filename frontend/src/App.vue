<script setup lang="ts">
import TheFooter from '@/components/layout/TheFooter.vue'
</script>

<template>
  <main class="main-content">
    <router-view></router-view>
  </main>
  <TheFooter />
</template>

<style>
html {
  height: 100%;
}
body {
  margin: 0;
  min-height: 100%;
  display: flex;
  background-color: var(--el-bg-color);
}

#app {
  width: 100%;
  max-width: 1920px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding: 0;
  font-family: 'Poppins', 'Noto Sans KR', sans-serif;
}

.main-content {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
</style>