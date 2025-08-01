<script setup lang="ts">
// <script> 부분은 이전과 동일하게 유지합니다.
import { ref, computed, onMounted, onUnmounted, defineProps, toRefs } from 'vue';
import { CircleClose } from '@element-plus/icons-vue';

export interface SessionExpiredAlertProps {
  initialSeconds?: number; // default 값이 있으므로 optional(?)로 지정
  onComplete: () => void; // 함수 타입 명시
}


const props = withDefaults(defineProps<SessionExpiredAlertProps>(), {
  initialSeconds: 10,
});

const { initialSeconds, onComplete } = toRefs(props);
const countdown = ref(initialSeconds.value);
let timer: any = null;

const progressPercent = computed(() => {
  return (countdown.value / initialSeconds.value) * 100;
});

onMounted(() => {
  timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer);
      onComplete.value();
    }
  }, 1000);
});

onUnmounted(() => {
  clearInterval(timer);
});
</script>

<template>
  <div class="session-alert-container-compact">
    <!-- 아이콘과 텍스트를 한 줄에 배치 -->
    <div class="content-wrapper">
      <div class="icon-wrapper">
        <el-icon :size="40" color="#F56C6C"><CircleClose /></el-icon>
      </div>
      <div class="text-wrapper">
        <h3 class="custom-title">세션 만료</h3>
        <p class="custom-message">
          안전을 위해 로그아웃됩니다. 다시 로그인해주세요.
        </p>
      </div>
    </div>

    <!-- 프로그레스 바 영역 -->
    <div class="progress-wrapper">
      <div class="progress-bar-container">
        <el-progress
            :percentage="progressPercent"
            :show-text="false"
            :stroke-width="12"
            color="#F56C6C"
        />
      </div>
      <div class="countdown-text">
        <strong>{{ countdown }}</strong>초 후 자동 이동
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 전체 컨테이너: 패딩을 줄이고 컴팩트하게 */
.session-alert-container-compact {
  display: flex;
  flex-direction: column;
  gap: 12px; /* 요소 간의 기본 간격 */
}

/* 상단 콘텐츠 (아이콘 + 텍스트) */
.content-wrapper {
  display: flex;
  align-items: center; /* 아이콘과 텍스트를 세로 중앙 정렬 */
  gap: 16px; /* 아이콘과 텍스트 사이 간격 */
}

.icon-wrapper {
  flex-shrink: 0; /* 아이콘이 찌그러지지 않도록 */
}

.text-wrapper {
  text-align: left;
}

.custom-title {
  font-size: 17px; /* 제목 크기 살짝 줄임 */
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0; /* 제목과 메시지 사이 간격 줄임 */
}

.custom-message {
  font-size: 14px;
  color: #606266;
  line-height: 1.5; /* 줄 간격 살짝 줄임 */
  margin: 0;
}

/* 하단 프로그레스 바 영역 */
.progress-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: #f9fafb; /* 살짝 배경색을 주어 구분 */
  padding: 8px 12px;
  border-radius: 8px;
}

.progress-bar-container {
  flex-grow: 1; /* 프로그레스 바가 남은 공간을 모두 차지하도록 */
}

/* 프로그레스 바 자체의 모서리를 둥글게 (내부 바 포함) */
.progress-bar-container .el-progress--line {
  border-radius: 6px;
  overflow: hidden;
}

.countdown-text {
  flex-shrink: 0;
  font-size: 13px;
  color: #606266;
  white-space: nowrap; /* 텍스트가 줄바꿈되지 않도록 */
}

.countdown-text strong {
  font-weight: 600;
  color: #F56C6C;
  font-size: 15px; /* 숫자 강조 */
}
</style>