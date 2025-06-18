<script setup lang="ts">
import {ref, onMounted, onUnmounted, nextTick} from 'vue'
import dayjs from 'dayjs'
import 'dayjs/locale/ko'

dayjs.locale('ko')
const date = ref(new Date())
const visible = ref(false)

const currentTime = ref(dayjs().format('A h:mm')) // A: 오전/오후, h: 12시간제, mm: 분
let timer: any

onMounted(() => {

  setTimeout(()=>{visible.value = true}, 500)

  timer = setInterval(() => {
    currentTime.value = dayjs().format('A h:mm')
  }, 1000)
})


const currentday = ref(dayjs().format('YYYY-MM-DD'))
onUnmounted(() => {
  clearInterval(timer)
})

</script>

<template>

  <el-tooltip
      content="Right Top prompts info"
      placement="right-start"
      :visible="visible"
      effect="light"
      style="width: 400px;"
  >
    <template #content>
      <VDatePicker
          v-model="date" transparent borderless
      ></VDatePicker>
    </template>

    <el-tag effect="light" style="margin-left: 4px; text-align: center; height: 92px; width: 110px;">

      <div style="display: inline-flex; align-items: center; font-size: 10px;">
        <el-icon style="margin-right: 4px" :size="12" @click="visible = !visible">
          <Calendar />
        </el-icon>
        현재시간
      </div>

      <div style="margin-top: 18px;">
        <el-text style="line-height: 1; font-size: 18px; color: #5c677d; font-weight: bold;">{{ currentTime }}</el-text>
      </div>

      <div style="margin-top: 8px;">
        <el-text style="line-height: 1; font-size: 12px; color: #5c677d; font-weight: bold;">{{ currentday }}</el-text>
      </div>
    </el-tag>
  </el-tooltip>

</template>

<style scoped>

</style>