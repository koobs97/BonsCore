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
import {Hide, QuestionFilled, View} from "@element-plus/icons-vue";
import {computed, nextTick, onMounted, onUnmounted, reactive, ref} from 'vue';
import {Api} from "@/api/axiosInstance";
import {ApiUrls} from "@/api/apiUrls";
import {ElIcon, ElMessage, ElMessageBox} from 'element-plus';
import {Common} from '@/common/common';
import {useRoute, useRouter} from 'vue-router';
import {userState, userStore} from '@/store/userStore';
import {Dialogs} from "@/common/dialogs";
import SocialLoginButtons from '@/components/login/SocialLoginButtons.vue';
import Setting from '@/assets/images/setting_icon.png';
import {useTheme} from '@/composables/useTheme';
import {useI18n} from 'vue-i18n';

// 커스텀 아이콘 이미지 임포트
import SunnyIcon from '@/assets/images/Sunny_icon.png';
import MoonIcon from '@/assets/images/Moon_icon.png';
import SystemIcon from '@/assets/images/System_icon.png';
import i18n from "@/i18n";

const { t, locale } = useI18n();

// router
const router = useRouter();
const route = useRoute();

// 테마 관리 로직
const { theme, setTheme } = useTheme();

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

// 팝오버 참조 및 닫기 함수
const popoverRef = ref();

/**
 * 테마 변경 시 호출되는 함수
 * @param newTheme 선택된 새 테마
 */
const onThemeChange = (newTheme: 'light' | 'dark' | 'system') => {
  setTheme(newTheme);
  // 선택 후 팝오버를 닫아주는 것이 사용자 경험에 좋습니다.
  popoverRef.value?.hide();
}

// pop over 제어 함수
const custromTrigger = () => {
  popoverRef.value = !popoverRef.value
}

// 화면진입 시
onMounted(async () => {

  // ElMessage clear
  await nextTick();
  ElMessageBox.close();
  ElMessage.closeAll();

  // 로그아웃 시 meesage
  if (route.query.status === 'logged-out') {
    await nextTick();
    ElMessage.success(t('logout.successMessage'));

    const newUrl = window.location.pathname;
    history.replaceState({}, '', newUrl);
  }

  // 로그인되어있는 경우 로그아웃
  const isLoggedIn = userStore().isLoggedIn;
  if (isLoggedIn) {
    setTimeout(()=>{
      userStore().delUserInfo();
      resetState();
    }, 1000);
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
    ElMessage({ message: t('login.messages.enterUserId'), grouping: true, type: 'error' })
    userIdInput.value.focus();
    return;
  }
  if(Common.isEmpty(password.value)) {
    ElMessage({ message: t('login.messages.enterPassword'), grouping: true, type: 'error' });
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
      await Common.setUser();

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

        const message = res.data.message;
        const backendResponse = res.data;
        const reason = backendResponse.reason;
        const backendMessage = backendResponse.message;
        const errorCode = backendResponse.code;

        let messageToShow = backendMessage; // 기본값은 백엔드 메시지

        // ErrorCode가 존재하면 i18n 번역을 시도합니다.
        if (errorCode) {
          const translationKey = `errors.${errorCode}`;
          const { t, te } = i18n.global;

          if (te(translationKey)) {
            messageToShow = t(translationKey); // 번역된 메시지 사용
          }
        }

        state.isProcessing = false;
        state.recaptchaToken = await Dialogs.showRecaptchaDialog(messageToShow);

      }
      // CAPTCHA가 필요 없는 다른 실패 사유(휴면 계정 등) 처리
      else {

        const message = res.data.message;
        const backendResponse = res.data;
        const reason = backendResponse.reason;
        const backendMessage = backendResponse.message;
        const errorCode = backendResponse.code;

        let messageToShow = backendMessage; // 기본값은 백엔드 메시지

        // ErrorCode가 존재하면 i18n 번역을 시도합니다.
        if (errorCode) {
          const translationKey = `errors.${errorCode}`;
          const { t, te } = i18n.global;

          if (te(translationKey)) {
            messageToShow = t(translationKey); // 번역된 메시지 사용
          }
        }

        switch (reason) {
            // 중복 로그인 시
          case 'DUPLICATE_LOGIN':
            await Dialogs.showDuplicateLoginConfirm(messageToShow)
                .then(() => { // '로그인' 버튼 클릭 시
                  state.isProcessing = false;
                  onClickLogin(true);
                }).catch((action) => {}).finally(() => {
                  state.isProcessing = false;
                });
            return;

            // 휴먼 계정일 시
          case 'DORMANT_ACCOUNT':
            await Dialogs.showDormantAccountNotice(
                t('login.dialogs.dormantAccount.title'),
                messageToShow
            )
                .then(() => { // '본인인증' 버튼 클릭 시
                  router.push({ path: '/VerifyIdentity', state: { type: 'DORMANT' } });
                }).catch((action) => {});
            return;

            // 비정상 로그인 탐지 시
          case 'ACCOUNT_VERIFICATION_REQUIRED':
            await Dialogs.showDormantAccountNotice(
                t('login.dialogs.abnormalLogin.title'),
                message
            )
                .then(() => { // '본인인증' 버튼 클릭 시
                  router.push({ path: '/VerifyIdentity', state: { type: 'ABNORMAL' } });
                }).catch((action) => {});
            return;

            // 기타 로그인 에러 메시지
          default:
            ElMessage({ message: messageToShow, grouping: true, type: 'error' });
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


/**
 * 언어 변경 핸들러 함수
 * @param newLang
 */
const onLanguageChange = (newLang: 'ko' | 'en') => {
  locale.value = newLang; // i18n의 locale 상태를 직접 변경
  localStorage.setItem('language', newLang); // 사용자의 선택을 브라우저에 저장
};
</script>

<template>
  <div class="login-container">
    <el-card class="login-card" shadow="never">
      <h2 class="login-title">{{ $t('login.title') }}</h2>

      <el-form class="login-form" @keydown.enter.prevent="onClickLogin(false)">
        <div class="form-options">
          <el-checkbox v-model="rememberId" :label="t('login.rememberId')" />

          <el-popover
              :visible="popoverRef"
              placement="right-start"
              :width="262"
              trigger="manual"
              popper-class="settings-popover"
          >
            <!-- 팝오버 내용 -->
            <div class="popover-content">
              <!-- 1. 테마 설정 섹션 -->
              <div class="setting-section">
                <span class="section-title">{{ $t('login.theme') }}</span>
                <el-radio-group
                    :model-value="theme"
                    @update:model-value="onThemeChange"
                    size="small"
                    class="theme-radio-group"
                >
                  <el-radio-button label="light">
                    <div class="theme-button-content">
                      <img :src="SunnyIcon" class="theme-icon" alt="라이트"/>
                      <span>{{ $t('login.theme_light') }}</span>
                    </div>
                  </el-radio-button>
                  <el-radio-button label="dark">
                    <div class="theme-button-content">
                      <img :src="MoonIcon" class="theme-icon" alt="다크"/>
                      <span>{{ $t('login.theme_dark') }}</span>
                    </div>
                  </el-radio-button>
                  <el-radio-button label="system">
                    <div class="theme-button-content">
                      <img :src="SystemIcon" class="theme-icon" alt="시스템"/>
                      <span>{{ $t('login.theme_system') }}</span>
                    </div>
                  </el-radio-button>
                </el-radio-group>
              </div>

              <el-divider />

              <!-- 2. 언어 설정 섹션 -->
              <div class="setting-section">
                <span class="section-title">{{ $t('login.language') }}</span>
                <div class="language-button-group">
                  <el-button
                      size="small"
                      :type="locale === 'ko' ? 'primary' : 'default'"
                      @click="onLanguageChange('ko')"
                  >
                    한국어
                  </el-button>
                  <el-button
                      size="small"
                      :type="locale === 'en' ? 'primary' : 'default'"
                      @click="onLanguageChange('en')"
                  >
                    English
                  </el-button>
                </div>
              </div>
            </div>

            <!-- 팝오버를 여는 버튼 -->
            <template #reference>
              <el-button link class="settings-btn" @click="custromTrigger">
                <el-icon :size="18"><img :src="Setting" alt="설정"/></el-icon>
              </el-button>
            </template>
          </el-popover>
        </div>

        <el-input
            v-model="userId"
            ref="userIdInput"
            class="login-input"
            :placeholder="t('login.userIdPlaceholder')"
            clearable
            v-byte-limit="50"
        />
        <el-input
            v-model="password"
            ref="passwordInput"
            class="login-input"
            :placeholder="t('login.passwordPlaceholder')"
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
            {{ $t('login.recommendedEnvironment') }}
          </el-button>
        </div>

        <div class="caps-lock-placeholder">
          <span :class="['caps-lock-warning', { 'visible': isCapsLockOn }]">
            {{ $t('login.capsLock') }}
          </span>
        </div>

        <el-button
            type="primary"
            class="login-button"
            @click="onClickLogin(false)"
        >
          {{ $t('login.loginButton') }}
        </el-button>
      </el-form>

      <div class="find-links">
        <el-button
            type="info"
            link
            @click="onClickToGoPage('FindId')"
        >
          {{ $t('login.findId') }}
        </el-button>
        <el-divider direction="vertical" />
        <el-button
            type="info"
            link
            @click="onClickToGoPage('FindPassword')"
        >
          {{ $t('login.findPassword') }}
        </el-button>
        <el-divider direction="vertical" />
        <el-button
            type="info"
            link
            @click="onClickToGoPage('VerifyIdentity', 'DORMANT')"
        >
          {{ $t('login.releaseDormant') }}
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
        {{ $t('login.signUp') }}
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
  display: flex;
  justify-content: space-between; /* 자식 요소들을 양쪽 끝으로 밀어냅니다. */
  align-items: center; /* 수직 중앙 정렬을 보장합니다. */
  margin-bottom: 6px;
}
.settings-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%; /* 원형 버튼으로 만듭니다. */
  transition: background-color 0.3s ease, transform 0.3s ease;
}
.settings-btn:hover {
  background-color: var(--el-fill-color-light);
}

.settings-btn img {
  width: 100%;
  height: 100%;
  transition: transform 0.3s ease; /* 부드러운 회전 효과 */
}

.settings-btn:hover img {
  transform: rotate(30deg);
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
    justify-content: flex-start;
    padding-top: 10vh;
  }

  .login-title {
    font-size: 1.5rem;
    margin-bottom: 24px;
  }

  .login-card {
    box-shadow: var(--el-box-shadow-light);
  }
}
</style>

<style>
.settings-popover {
  background: linear-gradient(var(--el-bg-color-overlay), var(--el-bg-color-overlay)) padding-box,
  linear-gradient(135deg, #3a3d40, #c8c8c8, #3a3d40) border-box !important;
  border: 2px solid transparent !important;
  border-radius: 18px !important;
  padding: 0 !important;
  box-shadow: 0 0 25px rgba(170, 170, 180, 0.3);
}

/* 기본 화살표 숨기기 */
.settings-popover .el-popper__arrow::before {
  display: none;
}

/* 내부 콘텐츠 영역 */
.settings-popover .popover-content {
  padding: 16px;
  background: transparent !important;
  z-index: 2;
}

.settings-popover .setting-section {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 16px;
}

/* 섹션 제목 */
.settings-popover .section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  flex-shrink: 0;
}

/* 구분선 */
.settings-popover .el-divider {
  margin: 12px 0;
  border-image: linear-gradient(to right, transparent, var(--el-border-color-darker), transparent) 1;
  border-top: 1px solid;
}

.settings-popover .theme-radio-group,
.settings-popover .language-button-group {
  flex-grow: 1; /* 핵심: 남은 공간을 모두 차지 */
  display: flex;
  width: 100%;
  flex-wrap: nowrap;
}

.settings-popover .theme-radio-group :deep(.el-radio-button__inner) {
  /* 기존 padding: 8px 5px; 에서 좌우 여백을 더 줄입니다. */
  padding: 8px 4px;
}

.settings-popover .theme-radio-group .el-radio-button {
  flex: 1;
}

.settings-popover .theme-button-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px; /* 기존 4px에서 더 줄임 */
  font-size: 12px;
  white-space: nowrap; /* 텍스트 줄바꿈 방지는 유지 */
}

.settings-popover .theme-icon {
  width: 16px;
  height: 16px;
}

.settings-popover .language-button-group {
  display: flex;
  width: 100%;
  gap: 8px;
}
.settings-popover .language-button-group .el-button {
  flex: 1;
  margin-left: 0 !important;
}

/* 설정 버튼 포커스 효과 제거 */
.settings-btn:focus,
.settings-btn:active {
  outline: none !important;
  box-shadow: none !important;
}

/* 다크 모드 스타일 */
html.dark .settings-btn img {
  filter: invert(0.85); /* 설정 아이콘 색상 반전 */
}

/* 다크모드에서 활성화된 테마 버튼 내부 아이콘/텍스트 색상 조정 */
html.dark .settings-popover .el-radio-button.is-active .theme-button-content {
  color: var(--el-bg-color-overlay);
}



@keyframes rotating-gradient {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.stylish-settings-popover {
  padding: 0 !important;
  border-radius: 14px !important; /* 내부 컨텐츠보다 살짝 큰 값 */
  border: none !important;
  background: transparent !important;
  position: relative;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2);
}

.stylish-settings-popover::before {
  content: '';
  position: absolute;
  z-index: 1;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: conic-gradient(from 0deg, #84fab0, #8fd3f4, #a18cd1, #fbc2eb, #84fab0);
  animation: rotating-gradient 4s linear infinite;
}

.stylish-settings-popover .el-popper__arrow::before {
  display: none; /* 기본 화살표 숨김 */
}

/* 팝오버 내부 콘텐츠 영역 */
.popover-content {
  position: relative;
  z-index: 2;
  margin: 2px; /* 테두리 두께 */
  padding: 8px;
  border-radius: 12px;
  background-color: var(--el-bg-color-overlay);
  backdrop-filter: blur(10px); /* 배경 블러 효과 */
}

.popover-content .setting-section {
  display: flex;
  flex-direction: column;
  gap: 12px; /* 섹션 내 아이템 간격 증가 */
}

.popover-content .section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.minimal-card-popover .button-group .el-button {
  /* 기본 버튼에 대한 공통 스타일 (선택 사항) */
  background-color: transparent;
}

/* 'primary' 타입이고 'plain' 속성을 가진 버튼의 스타일을 강제로 덮어쓰기 */
.minimal-card-popover .button-group .el-button--primary.is-plain {
  color: var(--el-color-white); /* 텍스트는 흰색으로 */
  background-color: var(--el-color-primary) !important; /* 배경색 채우기 */
  border-color: var(--el-color-primary) !important; /* 테두리 색상 통일 */
}

.popover-content .el-divider--horizontal {
  margin: 18px 0;
  border-top-color: var(--el-border-color-lighter);
}
.settings-btn:focus,
.settings-btn:active {
  outline: none !important;
  box-shadow: none !important;
}
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
  align-items: flex-start;
  gap: 20px;
  color: var(--el-text-color-regular);
  width: 100%;
  margin-left: 30px;
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