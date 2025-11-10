<script setup lang="ts">
/**
 * ========================================
 * 파일명   : Login.vue
 * ----------------------------------------
 * 설명     : 로그인
 * 작성자   : koobonsang
 * 버전     : 1.0
 * 작성일자 : 2025-01-30
 * ========================================
 */
import { Hide, QuestionFilled, View } from "@element-plus/icons-vue";
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { ElIcon, ElMessage, ElMessageBox } from 'element-plus';
import { Common } from '@/common/common';
import { useRoute, useRouter } from 'vue-router';
import { userState, userStore } from '@/store/userStore';
import { Dialogs } from "@/common/dialogs";
import SocialLoginButtons from '@/components/login/SocialLoginButtons.vue';

// router
const router = useRouter();
const route = useRoute();

/*
 * 패스워드는 ref(반응형 변수)로 새로고침하면 사라짐
 * state, pinia에 저장 시 XSS 공격에 취약
 * @type {import('vue').Ref<string>}
 */
const userId = ref();
const password = ref();
const rememberId = ref(true);

// ref focus 참조용 변수
const userIdInput = ref();
const passwordInput = ref();

// reactive 정의
const state = reactive({
  isVisible: false,
  isProcessing: false,                    // 화면 제어
  showCaptcha: false,                     // CAPTCHA 표시 여부 상태
  recaptchaToken: null as string | null,  // reCAPTCHA 토큰 저장
})

// caps lock 상태변수
const isCapsLockOn = ref(false);

// 비밀번호 아이콘 변경
const passwdIcon = computed(() => (state.isVisible ? View : Hide));
const passwdType = computed(() => (state.isVisible ? "text" : "password"));

// 비밀번호 입력 모드 전환
const togglePassword = () => {
  state.isVisible = !state.isVisible;
}

// 화면진입 시
onMounted(async () => {

  // ElMessage clear
  await nextTick();
  ElMessageBox.close();
  ElMessage.closeAll();
  console.log("Login page mounted: All previous messages have been cleared.");

  // 로그아웃 시 meesage
  if (route.query.status === 'logged-out') {
    await nextTick();
    ElMessage.success('성공적으로 로그아웃되었습니다.');

    const newUrl = window.location.pathname;
    history.replaceState({}, '', newUrl);
  }

  // 로그인되어있는 경우 로그아웃
  const isLoggedIn = userStore().isLoggedIn;
  if (isLoggedIn) {
    try {
      await Api.post(ApiUrls.LOGOUT, {}, true);
      setTimeout(()=>{
        userStore().delUserInfo();
      }, 1000);
    } catch (error){}
    finally {
      resetState();
    }
  }

  // CapsLock 탐지 이벤트리스너 등록
  window.addEventListener('keydown', updateCapsLockState);
  window.addEventListener('keyup', updateCapsLockState);

  resetState();
});

// 화면이탈 시
onUnmounted(() => {
  // CapsLock 탐지 이벤트리스너 해제
  window.removeEventListener('keydown', updateCapsLockState);
  window.removeEventListener('keyup', updateCapsLockState);
});

/**
 * 화면상태 세팅
 */
const resetState = () => {
  state.isVisible = false;
  state.isProcessing = false;

  if(rememberId.value && !Common.isEmpty(localStorage.getItem('userId'))) {
    userId.value = localStorage.getItem('userId');
    passwordInput.value?.focus();
  } else {
    localStorage.removeItem('userId');
    userIdInput.value?.focus();
  }
}
/**
 * CapsLock 탐지
 * @param event
 */
const updateCapsLockState = (event: KeyboardEvent) => {
  isCapsLockOn.value = event.getModifierState("CapsLock");
}

/**
 * 로그인 버튼 클릭 시 입력값 검증
 */
const validateInput = async () => {
  if(Common.isEmpty(userId.value)) {
    ElMessage({ message: '사용자 ID를 입력하세요.', grouping: true, type: 'error' })
    userIdInput.value.focus();
    return;
  }
  if(Common.isEmpty(password.value)) {
    ElMessage({ message: '비밀번호를 입력하세요.', grouping: true, type: 'error' });
    passwordInput.value.focus();
    return;
  }
  return true;
}

/**
 * 로그인 버튼 클릭 이벤트
 */
const onClickLogin = async (isForced: boolean) => {

  // 입력값 검증
  if(!await validateInput()) {
    return;
  }

  // CAPTCHA가 표시되었는데 체크하지 않은 경우
  if (state.showCaptcha && !state.recaptchaToken) {
    ElMessage.error("'로봇이 아닙니다.'를 체크해주세요.");
    return;
  }

  // 중복 요청 방지
  if (state.isProcessing) {
    ElMessage.warning("이미 요청 처리 중입니다. 잠시 후 다시 시도해주세요.");
    return;
  }

  // 서버에서 공개키 get
  const encryptedPassword = await Common.encryptPassword(password.value);
  state.isProcessing = true;

  try {
    const payload = {
      userId : userId.value,
      password : encryptedPassword,
      force: isForced,
      recaptchaToken: state.recaptchaToken
    };
    const res = await Api.post(ApiUrls.LOGIN, payload);
    console.log(res);

    /* 로그인에 성공 후 로직 */
    if(res.data.success) {
      // 로그인 성공 후에는 reCAPTCHA 상태를 초기화
      state.recaptchaToken = null;
      state.showCaptcha = false;

      // 실제로 유저 정보 불러와서 확인 (서버 호출)
      sessionStorage.setItem('accessToken', res.data.accessToken);

      // 유저정보 세팅
      const user = await Api.post(ApiUrls.GET_USER, { userId : userId.value }, true);
      const userInfo = user.data as userState
      sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
      userStore().setUserInfo(userInfo);

      // 아이디 기억하기
      if(rememberId.value) {
        localStorage.setItem('userId', userId.value);
      } else {
        localStorage.removeItem('userId');
      }

      // 사용자가 등록했던 임시파일 초기화
      await Api.post(ApiUrls.CLEAR_TEMP_FILE, {});

      // 구글 로그인 후 추가정보 입력이 필요한지 확인
      const additionalInfoRequired = res.data.additionalInfoRequired;
      if (additionalInfoRequired) {
        ElMessage.info('서비스를 이용하시려면 추가 정보 입력이 필요합니다.');
        await router.push("/oauth/additional-info");
      } else {
        // 추가 정보가 필요 없으면 메인 페이지로 이동
        await router.push("/");
      }
    }
    /* 로그인 실패 후 처리 로직 */
    else {

      // CAPTCHA가 필요한 경우
      if (res.data.captchaRequired) {

        state.isProcessing = false;
        state.recaptchaToken = await Dialogs.showRecaptchaDialog(res.data.message);

      }
      // CAPTCHA가 필요 없는 다른 실패 사유(휴면 계정 등) 처리
      else {

        const reason = res.data.reason;
        const message = res.data.message;

        switch (reason) {
          // 중복 로그인 시
          case 'DUPLICATE_LOGIN':
            await Dialogs.showDuplicateLoginConfirm(message)
                .then(() => { // '로그인' 버튼 클릭 시
                  state.isProcessing = false;
                  onClickLogin(true);
                }).catch((action) => {}).finally(() => {
                  state.isProcessing = false;
                });
            return;

          // 휴먼 계정일 시
          case 'DORMANT_ACCOUNT':
            await Dialogs.showDormantAccountNotice('휴면 계정 안내', message)
                .then(() => { // '본인인증' 버튼 클릭 시
                  router.push({ path: '/VerifyIdentity', state: { type: 'DORMANT' } });
                }).catch((action) => {});
            return;

          // 비정상 로그인 탐지 시
          case 'ACCOUNT_VERIFICATION_REQUIRED':
            await Dialogs.showDormantAccountNotice('비정상 로그인 감지', message)
                .then(() => { // '본인인증' 버튼 클릭 시
                  router.push({ path: '/VerifyIdentity', state: { type: 'ABNORMAL' } });
                }).catch((action) => {});
            return;

          // 기타 로그인 에러 메시지
          default:
            ElMessage({ message: message, grouping: true, type: 'error' });
        }
      }
    }
  } finally {
    if (!state.showCaptcha) {
      state.isProcessing = false;
    }
  }

}

/**
 * 화면이동
 * @param param
 * @param state
 */
const onClickToGoPage = (param: string, state?: string) => {
  if(state !== undefined)
    router.push({ path: '/VerifyIdentity', state: { type: 'DORMANT' } });
  else
    router.push("/" + param);
}

/**
 * 권장 해상도 및 브라우저 배율 안내 다이얼로그를 표시
 */
const showResolutionInfo = () => {
  Dialogs.showResolutionInfo();
}

const handleSocialLoginClick = (event: MouseEvent) => {
  if (state.isProcessing) {
    event.preventDefault();
    ElMessage.warning("이미 요청 처리 중입니다.");
    return;
  }
  state.isProcessing = true;
  setTimeout(() => {
    state.isProcessing = false;
  }, 2000);
}
</script>

<template>
  <div class="login-container">
    <el-card class="login-card" shadow="never">
      <h2 class="login-title">로그인</h2>

      <el-form class="login-form" @keydown.enter.prevent="onClickLogin(false)">
        <div class="form-options">
          <el-checkbox v-model="rememberId" label="아이디 기억하기" />
        </div>

        <el-input
            v-model="userId"
            ref="userIdInput"
            class="login-input"
            placeholder="사용자 ID"
            clearable
            v-byte-limit="50"
        />
        <el-input
            v-model="password"
            ref="passwordInput"
            class="login-input"
            placeholder="비밀번호"
            :type="passwdType"
            clearable
            v-byte-limit="50"
        >
          <template #append>
            <el-button @click="togglePassword">
              <el-icon><component :is="passwdIcon" /></el-icon>
            </el-button>
          </template>
        </el-input>

        <div class="extra-info-links">
          <el-button type="info" link @click="showResolutionInfo">
            <el-icon style="margin-right: 4px;"><QuestionFilled /></el-icon>
            권장 사용 환경
          </el-button>
        </div>

        <div class="caps-lock-placeholder">
          <span :class="['caps-lock-warning', { 'visible': isCapsLockOn }]">
            Caps Lock이 켜져 있습니다.
          </span>
        </div>

        <el-button
            type="primary"
            class="login-button"
            @click="onClickLogin(false)"
        >
          로그인
        </el-button>
      </el-form>

      <div class="find-links">
        <el-button
            type="info"
            link
            @click="onClickToGoPage('FindId')"
        >
          아이디 찾기
        </el-button>
        <el-divider direction="vertical" />
        <el-button
            type="info"
            link
            @click="onClickToGoPage('FindPassword')"
        >
          비밀번호 찾기
        </el-button>
        <el-divider direction="vertical" />
        <el-button
            type="info"
            link
            @click="onClickToGoPage('VerifyIdentity', 'DORMANT')"
        >휴면 계정 해제
        </el-button>
      </div>

      <!-- 소셜로그인 영역 -->
      <SocialLoginButtons />
    </el-card>

    <el-card class="signup-prompt-card" shadow="never">
      <el-button
          type="primary"
          link
          class="signup-link"
          @click="onClickToGoPage('SignUp')"
      >
        회원가입
      </el-button>
    </el-card>

  </div>
</template>

<style scoped>
/* 전체 페이지 레이아웃 */
.login-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;
  padding: 20px;
  box-sizing: border-box;
}
/* 로그인 카드 */
.login-card {
  width: 100%;
  max-width: 380px;
  box-sizing: border-box;
  padding: 4px 4px 0 4px;
  border-radius: 16px 16px 0 0;
  position: relative;
  z-index: 1;
}
.login-title {
  font-family: 'Poppins', sans-serif;
  font-weight: 800;
  letter-spacing: -0.5px;
  font-size: 1.85rem;
  color: var(--el-color-primary);
  text-align: center;
  margin-bottom: 30px;
}
.login-form {
  margin-top: 20px;
}
.form-options {
  text-align: left;
  margin-bottom: 15px;
}
.login-input {
  height: 45px;
  font-size: 0.9375rem;
  margin-bottom: 8px;
}
.login-input :deep(.el-input__inner) {
  height: 45px;
}
.login-button {
  width: 100%;
  height: 48px;
  font-weight: bold;
  font-size: 1rem;
  margin-top: 4px;
  color: var(--el-bg-color);
  background-color: var(--el-color-primary);
}
.find-links {
  margin-top: 12px;
  text-align: center;
}
/* 회원가입 카드 */
.signup-prompt-card {
  width: 100%;
  max-width: 380px;
  margin-top: 8px;
  text-align: center;
  padding: 2px 16px;
  box-sizing: border-box;
  border-radius: 0 0 16px 16px;
}
.signup-link {
  font-weight: bold;
  color: var(--el-color-primary) !important;
  font-size: 0.9375rem; /* 15px */
}
.caps-lock-placeholder {
  height: 8px; /* 경고 메시지를 담을 충분한 높이를 항상 차지 */
  display: flex;
  align-items: center; /* 내부 텍스트를 수직 중앙에 정렬 */
  text-align: left;
  margin-bottom: 8px;
  font-weight: bold;
}
.caps-lock-warning {
  color: #f56c6c;
  font-size: 0.75rem;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.3s ease;
}
.caps-lock-warning.visible {
  opacity: 1;
  visibility: visible;
}
.extra-info-links {
  margin-top: 0;
  margin-bottom: 16px;
  text-align: right;
}

.extra-info-links .el-button {
  font-size: 0.8125rem;
  color: var(--el-text-color-secondary);
  outline: none;
}

@media (max-width: 768px) {
  .login-container {
    /* 모바일에서는 위쪽으로 살짝 붙도록 정렬 변경 */
    justify-content: flex-start;
    padding-top: 10vh; /* 화면 높이의 10%만큼 위에서 띄움 */
  }

  .login-title {
    font-size: 1.5rem; /* 24px, 모바일에서 제목 크기 살짝 줄임 */
    margin-bottom: 24px;
  }

  .login-card {
    /* 모바일에서는 그림자 효과를 주어 입체감 부여 */
    box-shadow: var(--el-box-shadow-light);
  }
}
</style>

<style>
html.dark .el-checkbox__input.is-checked .el-checkbox__inner {
  background-color: var(--el-color-primary) !important;
  border-color: var(--el-color-primary) !important;
}
html.dark .el-checkbox__input.is-checked .el-checkbox__inner::after {
  border-color: var(--el-bg-color-page) !important;
}
html.dark .el-checkbox__input .el-checkbox__inner {
  border-color: var(--el-border-color-light) !important;
}

/* 애니메이션 정의: 그라데이션 회전 */
@keyframes rotating-gradient {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.resolution-info-box {
  --el-messagebox-width: 420px;
  border-radius: 12px !important;
  position: relative;
  overflow: hidden;
  padding: 0 !important;
  border: none !important;
  background: transparent !important;
}
.resolution-info-box::before {
  content: '';
  position: absolute;
  z-index: 1;
  top: -50%; left: -50%;
  width: 200%; height: 200%;
  background: conic-gradient(from 0deg, #e0c3fc, #8ec5fc, #e0c3fc);
  animation: rotating-gradient 4s linear infinite;
}
.resolution-info-box::after {
  content: '';
  position: absolute;
  z-index: 2;
  top: 2px; left: 2px; right: 2px; bottom: 2px;
  background: var(--el-bg-color-overlay);
  border-radius: 8px;
}
.resolution-info-box .el-message-box__header,
.resolution-info-box .el-message-box__content,
.resolution-info-box .el-message-box__btns {
  position: relative;
  z-index: 3;
  padding-bottom: 12px;
}
.resolution-info-box .el-message-box__header {
  margin-bottom: 6px;
  padding: 16px 25px 12px;
  border-bottom: 1px solid var(--el-border-color-light);
}
.resolution-info-box .el-message-box__content {
  padding: 24px 25px 0;
}
.resolution-info-box .el-message-box__title {
  font-size: 18px;
  font-weight: 600;
}
.resolution-info-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  color: var(--el-text-color-regular);
}
.resolution-info-content p {
  margin-bottom: 12px;
}
.resolution-info-content ul {
  list-style-type: disc;
  padding-left: 20px;
  margin: 0;
}
.resolution-info-content li {
  margin-bottom: 5px;
}
.resolution-info-box {
  --el-messagebox-width: 420px;
  border-radius: 8px !important;
}
.resolution-info-box .el-message-box__title {
  font-size: 18px;
  font-weight: 600;
}
.resolution-info-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  color: var(--el-text-color-regular);
}
.info-item {
  display: flex;
  align-items: center;
  gap: 16px;
}
.info-item .info-icon {
  color: var(--el-color-primary);
  background-color: var(--el-bg-color-page);
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.info-text {
  display: flex;
  flex-direction: column;
  text-align: left;
}
.info-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 2px;
}
.info-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.resolution-info-box .el-message-box__btns {
  margin-top: 24px;
}
@media (max-width: 768px) {
  .resolution-info-box {
    --el-messagebox-width: 90vw;
  }
}
.modern-recaptcha-dialog {
  --el-messagebox-width: auto;
  border-radius: 12px !important;
  position: relative;
  overflow: hidden;
  padding: 0 !important;
  border: none !important;
  background: transparent !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  width: 450px;
}
.modern-recaptcha-dialog::before {
  content: '';
  position: absolute;
  z-index: 1;
  top: -50%; left: -50%;
  width: 200%; height: 200%;
  background: conic-gradient(from 0deg, var(--el-color-primary-light-3), var(--el-color-success-light-3), var(--el-color-primary-light-3));
  animation: rotating-gradient 4s linear infinite;
}
.modern-recaptcha-dialog::after {
  content: '';
  position: absolute;
  z-index: 2;
  top: 2px; left: 2px; right: 2px; bottom: 2px;
  background: var(--el-bg-color-overlay);
  border-radius: 10px;
}
.modern-recaptcha-dialog .el-message-box__header {
  display: none;
}
.modern-recaptcha-dialog .el-message-box__content {
  position: relative;
  z-index: 3;
  padding: 0 !important;
}
.modern-recaptcha-dialog .el-message-box__btns {
  position: relative;
  z-index: 3;
  padding: 0 16px 16px 16px;
}
.modern-recaptcha-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px 24px 24px 24px;
  text-align: center;
}
.modern-recaptcha-content .dialog-icon {
  width: 48px;
  height: 48px;
  color: var(--el-color-primary);
}
.modern-recaptcha-content .dialog-title {
  font-size: 20px;
  font-weight: 600;
  margin: 12px 0 12px 0;
  color: var(--el-text-color-primary);
}
.modern-recaptcha-content .dialog-description {
  font-size: 14px;
  margin: 0 0 4px 0;
  color: var(--el-text-color-secondary);
}
.recaptcha-widget-container {
  width: 301px;
  height: 76px;
  overflow: hidden;
  border-radius: 2px;
  border: 1px solid var(--el-color-primary);
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 12px;
}
</style>