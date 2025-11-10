<template>
  <div class="social-login-section">
    <div class="social-login-divider">
      <span>{{ title }}</span>
    </div>
    <div class="social-login-buttons">
      <a href="http://localhost:8080/oauth2/authorization/kakao"
         :class="['social-button', 'kakao', { 'disabled': isProcessing }]"
         @click="handleSocialLoginClick">
        <img src="../../assets/images/kakao_login.png" alt="카카오 로그인">
      </a>
      <a href="http://localhost:8080/oauth2/authorization/naver"
         :class="['social-button', 'naver', { 'disabled': isProcessing }]"
         @click="handleSocialLoginClick">
        <img src="../../assets/images/naver_login.png" alt="네이버 로그인">
      </a>
      <a href="http://localhost:8080/oauth2/authorization/google"
         :class="['social-button', 'google', { 'disabled': isProcessing }]"
         @click="handleSocialLoginClick">
        <img src="../../assets/images/google_login.png" alt="구글 로그인">
      </a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';

// props를 정의하여 부모 컴포넌트로부터 텍스트를 전달받을 수 있도록 합니다.
defineProps({
  title: {
    type: String,
    default: '간편 로그인'
  }
});

const isProcessing = ref(false);

const handleSocialLoginClick = (event: MouseEvent) => {
  if (isProcessing.value) {
    event.preventDefault();
    ElMessage.warning("이미 요청 처리 중입니다.");
    return;
  }
  isProcessing.value = true;
  // 페이지 이동이 일어나므로 굳이 false로 되돌릴 필요는 없습니다.
  setTimeout(() => {
    isProcessing.value = false;
  }, 2000);
}
</script>

<style scoped>
/* Login.vue에 있던 social-login 관련 CSS를 그대로 가져옵니다. */
.social-login-section {
  margin-top: 30px; /* 상단 간격을 살짝 조정 */
}
.social-login-divider {
  display: flex;
  align-items: center;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 0.8125rem;
  margin-bottom: 16px;
}
.social-login-divider::before,
.social-login-divider::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid var(--el-border-color-light);
}
.social-login-divider span {
  padding: 0 12px;
}
.social-login-buttons {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 14px;
}
.social-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px; /* 너비와 높이를 통일하여 원형 버튼으로 */
  height: 48px;
  border-radius: 50%; /* 원형으로 변경 */
  text-decoration: none;
  transition: all 0.2s ease-in-out;
  box-shadow: var(--el-box-shadow-lighter);
  background-color: var(--el-bg-color);
}
.social-button:hover {
  transform: translateY(-2px);
  box-shadow: var(--el-box-shadow-light);
}
.social-button img {
  height: 24px; /* 아이콘 크기 살짝 키움 */
}

/* 개별 버튼 스타일은 이미지 외 불필요한 부분 제거 */
.social-button.kakao { background-color: #FEE500; }
.social-button.naver { background-color: #03C75A; }
.social-button.naver img { filter: brightness(0) invert(1); }
.social-button.google { background-color: #FFFFFF; }

.social-button.disabled {
  pointer-events: none;
  opacity: 0.6;
}
</style>