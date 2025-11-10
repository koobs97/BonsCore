<script setup lang="ts">
import { useRouter } from "vue-router";
import { Message } from '@element-plus/icons-vue'

const router = useRouter();

const onClickToGoPage = (path: string) => {
  router.push("/" + path);
}
</script>

<template>
  <div class="signup-container">
    <el-card class="signup-card" shadow="never">

      <a href="/" class="logo-container">
        <div class="logo-icon-wrapper">
          <div class="logo-icon"></div>
        </div>
        <div>
          <span class="logo-main-text">BONS</span>
          <span class="logo-sub-text">Project</span>
        </div>
      </a>

      <h2 class="main-title">시작하기</h2>
      <p class="description">가입 방법을 선택해주세요.</p>

      <div class="method-buttons">
        <!-- 소셜 가입 버튼들 -->
        <a href="http://localhost:8080/oauth2/authorization/kakao" class="method-button kakao">
          <div class="icon-area">
            <img src="@/assets/images/kakao_login.png" alt="카카오 아이콘" />
          </div>
          <div class="text-area">
            <span>카카오로 시작하기</span>
          </div>
        </a>
        <a href="http://localhost:8080/oauth2/authorization/naver" class="method-button naver">
          <div class="icon-area">
            <img src="@/assets/images/naver_login.png" alt="네이버 아이콘" />
          </div>
          <div class="text-area">
            <span>네이버로 시작하기</span>
          </div>
        </a>
        <a href="http://localhost:8080/oauth2/authorization/google" class="method-button google">
          <div class="icon-area">
            <img src="@/assets/images/google_login.png" alt="구글 아이콘" />
          </div>
          <div class="text-area">
            <span>구글로 시작하기</span>
          </div>
        </a>

        <el-divider class="divider" />

        <a @click="onClickToGoPage('EmailSignUp')" class="method-button email">
          <div class="icon-area">
            <img src="@/assets/images/email_login.png" alt="이메일가입 아이콘" />
          </div>
          <div class="text-area">
            <span>이메일로 가입하기</span>
          </div>
        </a>
      </div>

      <div class="signup-prompt-card">
        <el-text>이미 계정이 있으신가요?</el-text>
        <el-button type="primary" link class="signup-link" @click="onClickToGoPage('Login')">로그인</el-button>
      </div>

    </el-card>
  </div>
</template>

<style scoped>
/* Main.vue에서 가져온 로고 스타일 */
.logo-container {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  text-decoration: none;
  margin-bottom: 24px; /* 아래 컨텐츠와의 간격 */
}
.logo-icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--el-color-primary-light-9);
  transition: transform 0.2s ease;
}
.logo-icon {
  width: 18px;
  height: 18px;
  border-radius: 6px;
  background-color: var(--el-color-primary);
}
.logo-main-text {
  font-family: 'Poppins', sans-serif;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.2;
  transition: color 0.2s ease;
  letter-spacing: 0.5px;
}
.logo-sub-text {
  font-family: 'Poppins', sans-serif;
  font-size: 0.75rem;
  text-align: left;
  margin-left: 4px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
  line-height: 1;
}
.logo-container:hover .logo-icon-wrapper {
  transform: rotate(10deg) scale(1.05);
}
.logo-container:hover .logo-main-text {
  color: var(--el-color-primary);
}

/* --- 기존 SignUp.vue 스타일은 유지 --- */
.signup-container {
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--el-bg-color);
}
.signup-card {
  width: 100%;
  max-width: 420px;
  padding: 24px 32px 0 32px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.main-title {
  text-align: center;
  font-size: 1.8rem;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--el-text-color-primary);
}
.description {
  text-align: center;
  color: var(--el-text-color-secondary);
  margin-bottom: 32px;
}
.method-buttons {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}
.method-button {
  display: grid;
  grid-template-columns: 50px 1fr; /* 1. 아이콘 영역(50px 고정) | 2. 텍스트 영역(나머지 전체) */
  align-items: center;
  width: 100%;
  height: 50px;
  border-radius: 8px;
  font-size: 1rem;
  font-family: 'Noto Sans KR', 'Poppins', sans-serif;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  padding: 0 24px;
  box-sizing: border-box;
}
.icon-area {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}
.icon-area img, .icon-area .el-icon {
  width: 24px;
  height: 24px;
}
.method-button img, .method-button .el-icon {
  width: 22px;
  height: 22px;
  margin-right: 12px;
}
.method-button:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}
.method-button.kakao { background-color: #FEE500; color: #191919; }
.method-button.naver { background-color: #03C75A; color: white; }
.method-button.naver img { filter: brightness(0) invert(1); }
.method-button.google { background-color: #FFFFFF; color: #1F1F1F; border-color: #DADCE0; }
.method-button.email {
  color: var(--el-bg-color);
  background-color: var(--signup-color-email);
  cursor: pointer;
}

.divider {
  margin: 12px 0 12px 0;
}

.signup-prompt-card {
  width: 100%;
  max-width: 700px;
  margin-top: 12px;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 4px 4px;
  box-sizing: border-box;
  height: 30px;
  gap:4px;
}
.signup-link {
  color: var(--el-color-primary) !important;
}
</style>