<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'

const currentTime = ref(dayjs().format('HH:mm:ss'))
let timer: any

onMounted(() => {
  timer = setInterval(() => {
    currentTime.value = dayjs().format('HH:mm:ss')
  }, 1000)
})


const currentday = ref(dayjs().format('YYYY-MM-DD'))
onUnmounted(() => {
  clearInterval(timer)
})
</script>

<template>
  <div>
    <el-tag effect="light" style="margin-left: 4px; text-align: center; height: 92px; width: 120px;">

      <div style="display: inline-flex; align-items: center">
        <el-icon style="margin-right: 4px" :size="12">
          <Calendar />
        </el-icon>
        Calendar
      </div>

      <div style="margin-top: 12px;">
        <el-text style="line-height: 1; font-size: 18px; color: #5c677d; font-weight: bold;">{{ currentTime }}</el-text>
      </div>

      <div style="margin-top: 8px;">
        <el-text style="line-height: 1; font-size: 12px; color: #5c677d; font-weight: bold;">{{ currentday }}</el-text>
      </div>
    </el-tag>
  </div>
</template>

<style scoped>

</style>