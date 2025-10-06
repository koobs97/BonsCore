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
import { Hide, Monitor, View, ZoomIn } from "@element-plus/icons-vue";
import { computed, h, nextTick, onMounted, reactive, ref } from 'vue';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { ElIcon, ElMessage, ElMessageBox } from 'element-plus';
import { Common } from '@/common/common';
import { useRouter } from 'vue-router';
import { userState, userStore } from '@/store/userStore';
import CustomConfirm from "@/components/MessageBox/CustomConfirm.vue";
import DormantAccountNotice from "@/components/MessageBox/DormantAccountNotice.vue";

// router
const router = useRouter();

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
  isProcessing: false, // 화면 제어
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
  setTimeout(() => {
    ElMessageBox.close();
    ElMessage.closeAll();
    console.log("Login page mounted: All previous messages have been cleared.");

  }, 0);

  const isLoggedIn = userStore().isLoggedIn;
  if (isLoggedIn) {
    try {
      await Api.post(ApiUrls.LOGOUT, {}, true);
      setTimeout(()=>{
        userStore().delUserInfo();
        sessionStorage.clear();
      }, 1000);
    } catch (error){}
    finally {
      resetState();
    }
  }

  resetState();
})

/**
 * 화면상태 세팅
 */
const resetState = () => {
  state.isVisible = false;
  state.isProcessing = false;

  if(rememberId.value && !Common.isEmpty(sessionStorage.getItem('userId'))) {
    userId.value = sessionStorage.getItem('userId');
    passwordInput.value?.focus();
  } else {
    sessionStorage.removeItem('userId');
    userIdInput.value?.focus();
  }
}

/**
 * 비밀번호 입력 시 Caps Lock 상태를 확인하는 함수
 * @param {KeyboardEvent} event
 */
const checkCapsLock = (event: KeyboardEvent) => {
  isCapsLockOn.value = event.getModifierState("CapsLock");
};

/**
 * Caps Lock 경고 메시지를 숨긴다.
 * 이 함수는 입력 필드나 윈도우가 포커스를 잃었을 때 호출된다.
 */
const hideCapsLockWarning = () => {
  isCapsLockOn.value = false;
};

/**
 * 로그인 버튼 클릭 시 입력값 검증
 */
const validateInput = async () => {
  if(Common.isEmpty(userId.value)) {
    ElMessage({ message: '사용자ID를 입력하세요.', grouping: true, type: 'error' })
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
  if(await validateInput()) {

    if (state.isProcessing) {
      ElMessage.warning("이미 요청 처리 중입니다. 잠시 후 다시 시도해주세요.");
      return; // 즉시 함수 종료
    }

    // 서버에서 공개키 get
    const encryptedPassword = await Common.encryptPassword(password.value);
    state.isProcessing = true;

    try {
      const res = await Api.post(ApiUrls.LOGIN, { userId : userId.value, password : encryptedPassword, force: isForced });
      if(res.data.success) {

        // 실제로 유저 정보 불러와서 확인 (서버 호출)
        sessionStorage.setItem('accessToken', res.data.accessToken);

        // 유저정보 세팅
        const user = await Api.post(ApiUrls.GET_USER, { userId : userId.value }, true);
        const userInfo = user.data as userState

        sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
        userStore().setUserInfo(userInfo);

        // 아이디 기억하기
        if(rememberId.value) {
          sessionStorage.setItem('userId', userId.value);
        } else {
          sessionStorage.removeItem('userId');
        }

        // 사용자가 등록했던 임시파일 초기화
        await Api.post(ApiUrls.CLEAR_TEMP_FILE, {});

        // main 화면 진입
        await router.push("/");

      } else {
        console.log('login failed -> {}', res);

        // 중복 로그인 시
        if(res.data.reason === 'DUPLICATE_LOGIN') {

          await ElMessageBox.confirm(
              // message 옵션에 h(컴포넌트, props) 전달
              h(CustomConfirm, {
                title: '중복 로그인 감지',
                message: res.data.message, // 서버에서 받은 메시지 ("...<br>...")
              }),
              // title 옵션은 빈 문자열로 두거나, h()를 사용하면 무시됨
              '',
              {
                // 버튼 텍스트는 그대로 유지
                confirmButtonText: '로그인',
                cancelButtonText: '취소',

                // 추가적인 스타일링을 위한 클래스
                customClass: 'custom-message-box',

                // 아이콘을 컴포넌트 안에서 직접 그리므로, 기본 아이콘은 숨김
                showClose: false,
                distinguishCancelAndClose: true, // ESC나 닫기 버튼을 취소와 구분
                type: '' // 기본 'warning' 타입 아이콘을 숨기기 위해 빈 값으로 설정
              }
          ).then(() => {
            // '로그인' 버튼 클릭 시
            state.isProcessing = false;
            onClickLogin(true);
          }).catch((action) => {
            // '취소' 버튼 클릭 또는 ESC, 닫기 버튼 클릭
            if (action === 'cancel') {
              ElMessage.info('로그인을 취소했습니다.');
            }
          }).finally(() => {
            state.isProcessing = false;
          });
        }
        else if (res.data.reason === 'DORMANT_ACCOUNT') {
          await ElMessageBox.confirm(
              h(DormantAccountNotice, { // 새로 만든 컴포넌트 사용
                title: '휴면 계정 안내',
                message: res.data.message // 서버에서 받은 메시지
              }),
              '',
              {
                confirmButtonText: '본인인증 하러가기',
                cancelButtonText: '닫기',
                customClass: 'custom-message-box',
                showClose: false,
                distinguishCancelAndClose: true,
                type: ''
              }
          ).then(() => {
            // '본인인증' 버튼 클릭 시
            router.push('/VerifyIdentity'); // 본인인증 페이지로 이동
          }).catch((action) => {
            // '닫기' 버튼 클릭 또는 ESC, 닫기 버튼 클릭
            if (action === 'cancel') {
              ElMessage.info('로그인을 취소했습니다.');
            }
          });
        }

      }
    } finally {
      state.isProcessing = false;
    }
  }
}

/**
 * 화면이동
 * @param param
 */
const onClickToGoPage = (param: string) => {
  router.push("/" + param);
}
const showResolutionInfo = () => {
  ElMessageBox.alert(
      // h() 함수를 사용하여 아이콘과 텍스트가 포함된 복잡한 구조를 생성
      h('div', { class: 'resolution-info-content' }, [
        h('div', { class: 'info-item' }, [
          h(ElIcon, { class: 'info-icon', size: 20 }, () => h(Monitor)), // 모니터 아이콘
          h('div', { class: 'info-text' }, [
            h('span', { class: 'info-label' }, '최적 해상도'),
            h('span', { class: 'info-value' }, '1920 x 1080'),
          ]),
        ]),
        h('div', { class: 'info-item' }, [
          h(ElIcon, { class: 'info-icon', size: 20 }, () => h(ZoomIn)), // 돋보기(배율) 아이콘
          h('div', { class: 'info-text' }, [
            h('span', { class: 'info-label' }, '브라우저 배율'),
            h('span', { class: 'info-value' }, '125%'),
          ]),
        ]),
      ]),
      '권장 사용 환경', // Title
      {
        confirmButtonText: '확인',
        center: true,
        customClass: 'resolution-info-box', // 커스텀 스타일 클래스,
        showClose: false,
      }
  )
}
</script>

<template>
  <!-- 전체 레이아웃을 감싸는 컨테이너 추가 -->
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
            v-byte-limit="50"
        />
        <el-input
            v-model="password"
            ref="passwordInput"
            class="login-input"
            placeholder="비밀번호"
            :type="passwdType"
            v-byte-limit="50"
            @keydown="checkCapsLock"
            @blur="hideCapsLockWarning"
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
          <!-- isCapsLockOn 상태에 따라 'visible' 클래스를 동적으로 제어. -->
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
        <el-button type="info" link @click="onClickToGoPage('FindId')">아이디 찾기</el-button>
        <el-divider direction="vertical" />
        <el-button type="info" link @click="onClickToGoPage('FindPassword')">비밀번호 찾기</el-button>

      </div>
    </el-card>

    <el-card class="signup-prompt-card" shadow="never">
      <el-button type="primary" link class="signup-link" @click="onClickToGoPage('SignUp')">
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
  min-height: calc(100vh - 100px);
}
/* 로그인 카드 */
.login-card {
  width: 380px;
  padding: 4px;
  box-sizing: border-box; /* 패딩이 너비에 영향을 주지 않도록 설정 */
}
.login-title {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  letter-spacing: 1px;
  font-size: 28px;
  color: var(--el-color-primary);
  text-align: center;
  margin-bottom: 30px;
}
/* 로그인 폼 */
.login-form {
  margin-top: 20px;
}
.form-options {
  text-align: left;
  margin-bottom: 15px;
}
.login-input {
  height: 45px;
  font-size: 15px;
  margin-bottom: 8px;
}
/* Element Plus의 내부 스타일을 덮어쓰기 위해 더 구체적인 선택자 사용 */
.login-input :deep(.el-input__inner) {
  height: 45px;
}
.login-button {
  width: 100%;
  height: 48px;
  font-weight: bold;
  font-size: 16px;
  margin-top: 16px;
  color: var(--el-bg-color);
}
.find-links {
  margin-top: 20px;
  text-align: center;
}
/* 회원가입 카드 */
.signup-prompt-card {
  width: 380px;
  margin-top: 12px;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px 8px;
  box-sizing: border-box;
}
.signup-link {
  font-weight: bold;
  color: var(--el-color-primary) !important;
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
  font-size: 12px;
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
  margin-bottom: 12px;
  text-align: right;
}

.extra-info-links .el-button {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  outline: none;
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

/* 1. 최상위 박스 (애니메이션의 기준틀 역할) */
.resolution-info-box {
  --el-messagebox-width: 420px;
  border-radius: 12px !important;
  position: relative;
  overflow: hidden;
  padding: 0 !important;
  border: none !important;
  background: transparent !important;
}

/* 2. 회전하는 그라데이션 배경 (레이어 1) */
.resolution-info-box::before {
  content: '';
  position: absolute;
  z-index: 1;
  top: -50%; left: -50%;
  width: 200%; height: 200%;

  /* 라이트 모드: 은은한 파스텔 톤 */
  background: conic-gradient(from 0deg, #e0c3fc, #8ec5fc, #e0c3fc);
  animation: rotating-gradient 4s linear infinite;
}

/* 3. 내부 배경색을 덮어 테두리 효과를 만드는 마스크 (레이어 2) */
.resolution-info-box::after {
  content: '';
  position: absolute;
  z-index: 2; /* 그라데이션 위에 위치 */
  top: 2px; left: 2px; right: 2px; bottom: 2px; /* 2px 두께의 테두리 생성 */

  background: var(--el-bg-color-overlay); /* 내부 배경색 */
  border-radius: 8px; /* 외부보다 살짝 작은 둥근 모서리 */
}

/* 4. 실제 컨텐츠 (레이어 3) */
/* 컨텐츠가 마스크 위에 보이도록 z-index를 설정 */
.resolution-info-box .el-message-box__header,
.resolution-info-box .el-message-box__content,
.resolution-info-box .el-message-box__btns {
  position: relative;
  z-index: 3;
  padding-bottom: 12px;
}

.resolution-info-box .el-message-box__header {
  margin-bottom: 6px; /* 제목과 내용 사이 간격 확보 */
  padding: 16px 25px 12px;
  border-bottom: 1px solid var(--el-border-color-light); /* 제목 아래 구분선 추가 */
}
.resolution-info-box .el-message-box__content {
  padding: 24px 25px 0;
}
.resolution-info-box .el-message-box__title {
  font-size: 18px;
  font-weight: 600;
}

.resolution-info-content {
  color: var(--el-text-color-regular);
  text-align: left;
  line-height: 1.7;
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
  --el-messagebox-width: 420px; /* 다이얼로그 너비 조정 */
  border-radius: 8px !important; /* 모서리를 더 둥글게 */
}

.resolution-info-box .el-message-box__title {
  font-size: 18px;
  font-weight: 600;
}

.resolution-info-content {
  display: flex;
  flex-direction: column;
  gap: 20px; /* 각 정보 항목 사이의 수직 간격 */
  color: var(--el-text-color-regular);
}

.info-item {
  display: flex;
  align-items: center; /* 아이콘과 텍스트를 수직 중앙 정렬 */
  gap: 16px; /* 아이콘과 텍스트 사이의 간격 */
}

.info-item .info-icon {
  color: var(--el-color-primary); /* 아이콘 색상 */
  background-color: var(--el-bg-color-page); /* 아이콘 배경색 */
  padding: 8px;
  border-radius: 50%; /* 원형 배경 */
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
  color: var(--el-text-color-secondary); /* 레이블 색상을 약간 연하게 */
  margin-bottom: 2px;
}

.info-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary); /* 값을 더 강조 */
}

.resolution-info-box .el-message-box__btns {
  margin-top: 24px; /* 내용과 버튼 사이 간격 */
}
</style>