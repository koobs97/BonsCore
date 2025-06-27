<script setup lang="ts">
import { ref, onMounted, onUnmounted, defineProps, toRefs } from 'vue';

// 부모로부터 초깃값과 콜백 함수를 받기 위한 props 정의
const props = defineProps({
  initialSeconds: {
    type: Number,
    default: 10
  },
  onComplete: {
    type: Function,
    required: true
  }
});

// props를 반응성을 유지하며 구조분해
const { initialSeconds, onComplete } = toRefs(props);

const countdown = ref(initialSeconds.value);
let timer = null as any;

// 컴포넌트가 화면에 마운트되면 타이머 시작
onMounted(() => {
  timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer);
      // 카운트다운이 끝나면 부모에게 받은 onComplete 함수를 호출
      onComplete.value();
    }
  }, 1000);
});

// 컴포넌트가 사라질 때(unmount) 반드시 타이머를 정리하여 메모리 누수 방지
onUnmounted(() => {
  clearInterval(timer);
});

</script>

<template>
  <div>
    <p>세션이 만료되었습니다.</p>
    <p>
      로그인 화면으로 이동합니다. (
      <!-- countdown 값이 바뀔 때마다 이 부분이 자동으로 업데이트됩니다 -->
      <span style="color: #e6a23c; font-weight: bold;">{{ countdown }}</span>
      초)
    </p>
  </div>
</template>

<style scoped>

</style>