<script setup lang="ts">
import { onActivated } from 'vue';
import { useRouter } from 'vue-router';
import { userStore } from "./store/userStore";

const userStoreObj = userStore();
const router = useRouter();

onActivated(() => {
  if(!userStoreObj.isLoggedIn) {
    router.push('/login')
  }
  else {
    router.push('/')
  }
})

</script>

<template>
  <router-view></router-view>
</template>

<style scoped>

</style>
