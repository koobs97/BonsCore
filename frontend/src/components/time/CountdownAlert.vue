<script setup lang="ts">
import { ref, onMounted, onUnmounted, defineProps, toRefs } from 'vue';
import { useI18n } from 'vue-i18n';

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

const { initialSeconds, onComplete } = toRefs(props);
const { t } = useI18n();

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
    <!-- 제목: 세션이 만료되었습니다. -->
    <p class="title">{{ t('session.expired.title') }}</p>
    <!-- 내용: 로그인 화면으로 이동합니다. ( N 초 ) -->
    <p class="desc">
      {{ t('session.expired.message') }} (
      <span class="countdown-number">{{ countdown }}</span>
      {{ t('session.expired.unit') }})
    </p>
  </div>
</template>
<style scoped>
</style>