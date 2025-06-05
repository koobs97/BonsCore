<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'

const currentTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'))
let timer: number

onMounted(() => {
  timer = setInterval(() => {
    currentTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
  }, 1000)
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<template>
  <div>
    <el-tag effect="light" style="margin-left: 4px; display: inline-flex; align-items: center;">
      <el-icon style="vertical-align: middle; margin-right: 4px;">
        <Clock />
      </el-icon>
      <span style="line-height: 1;">{{ currentTime }}</span>
    </el-tag>
  </div>
</template>

<style scoped>

</style>