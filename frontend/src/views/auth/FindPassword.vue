<script setup lang="ts">
/**
 * ========================================
 * 파일명   : FindPassword.vue
 * ----------------------------------------
 * 설명     : 비밀번호 찾기
 * 작성자   : koobonsang
 * 버전     : 1.0
 * 작성일자 : 2025-07-23
 * ========================================
 */
import { ref, reactive, computed, onMounted, nextTick } from 'vue';
import { onBeforeRouteLeave, useRouter } from 'vue-router';
import { ElAlert, ElLoading, ElMessage } from 'element-plus';
import {
  User,
  Lock,
  Key,
  CircleCheckFilled,
  QuestionFilled,
  Timer,
  MoreFilled,
  Promotion, Right
} from '@element-plus/icons-vue';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { Common } from '@/common/common';
import { Dialogs } from "@/common/dialogs";
import { useI18n } from "vue-i18n";

// i18n
const { t } = useI18n();

// Vue 라우터 인스턴스
const router = useRouter();

// 타이머 상태
const state = reactive({
  totalSeconds: 180, // 전체 남은 시간을 초 단위로 관리
  timerId: null as any | null, // setInterval의 ID를 저장하기 위한 변수
  isVerified: false,
})

// UI 흐름 제어를 위한 상태 변수
const currentStep = ref(0); // 0: 방법 선택, 1: 본인 인증, 2: 비밀번호 재설정, 3: 완료
const findMethod = ref<'email' | 'security' | null>(null); // 'email' 또는 'security'
const isCodeSent = ref(false); // 인증번호 전송 여부

// 버튼 loading
const emailLoading = ref(false);
const securityQuestionLoading = ref(false);

// focus
const userId = ref();
const newPassword = ref();

// token
const token = ref();

// 폼 데이터 모델
const formData = reactive({
  userId: '',
  userName: '',
  email: '',
  authCode: '',
  newPassword: '',
  confirmPassword: '',
});

// 보안 질문 관련 상태
const securityQuestion = ref('');
const securityAnswer = ref('');
const showNoQuestionAlert = ref(false);

// 화면진입 시
onMounted(() => {
})

/**
 * 뒤로가기/앞으로가기 시 실행할 작업
 */
onBeforeRouteLeave(async(to, from, next) => {

  /* 인증 전에 페이지 이탈 시 초기화 */
  if(state.timerId) {
    clearInterval(state.timerId);
    state.isVerified = true;
  }

  next(); // 다음 단계로 진행
})

/**
 * 찾기 방법 선택
 * @param method
 */
const selectMethod = (method: 'email' | 'security') => {
  findMethod.value = method;
  currentStep.value = 1;
  nextTick(() => {
    if (userId.value) {
      userId.value.focus();
    }
  });
};


/**
 * 보안 질문 가져오기
 */
const fetchUserSecurityQuestion = async () => {
  if (!formData.userId) {
    ElMessage.error(t('findPassword.messages.enterUserIdFirst'));
    return;
  }
  securityQuestionLoading.value = true;
  showNoQuestionAlert.value = false;
  try {
    // [API 호출] 사용자의 ID를 기반으로 설정된 보안 질문을 가져옵니다.
    const response = await Api.post(ApiUrls.GET_USER_PASSWORD_HINT, { userId: formData.userId });

    // 보안 질문이 있는 경우와 없는 경우 분리
    if (response) {
      securityQuestion.value = response;
    } else {
      // 보안 질문이 없는 경우, 경고창을 표시
      showNoQuestionAlert.value = true;
      securityQuestion.value = '';
    }
  } catch (error) {
    securityQuestion.value = '';
  } finally {
    securityQuestionLoading.value = false;
  }
};

/**
 * 보안 질문 답변 확인
 */
const verifySecurityAnswer = async () => {
  if (!securityAnswer.value) {
    ElMessage.error(t('findPassword.messages.enterAnswer'));
    return;
  }
  try {
    const response = await Api.post(ApiUrls.VALIDATE_PASSWORD_HINT, {
      userId: formData.userId,
      passwordHintAnswer: securityAnswer.value,
    });

    if (response.data) {
      token.value = response.data.token;
      currentStep.value = 2; // 비밀번호 재설정 단계로 이동
      ElMessage.success(t('findPassword.messages.verificationComplete'));
      nextTick(() => {
        newPassword.value.focus();
      });
    } else {
      ElMessage.error(t('findPassword.messages.answerMismatch'));
    }
  } catch (error) {
    ElMessage.error(t('findPassword.messages.answerCheckError'));
  }
};


/**
 * 인증번호 전송 함수
 * 실제로는 API를 호출하여 인증번호를 발송합니다.
 */
const sendAuthCode = async () => {

  // 간단한 유효성 검사
  if (!formData.userId || !formData.userName || !formData.email) {
    ElMessage.error(t('findPassword.messages.enterAllFields'));
    return;
  }

  try {
    emailLoading.value = true;
    await Api.post(ApiUrls.SEND_MAIL, { nonMaskedId: formData.userId, userName: formData.userName, email: formData.email, type: 'PW' });
    ElMessage({
      message: t('findPassword.messages.emailSent'),
      grouping: true,
      type: 'success',
    });
  } finally {
    emailLoading.value = false;
  }

  // 인증번호 입력란 비활성화 해제
  isCodeSent.value = true;
  state.isVerified = false;
  startTimer();
};

/**
 * 인증번호 확인 및 다음 단계로 이동 함수며
 * 실제로는 API를 호출하여 인증번호의 유효성을 검증.
 */
const verifyAndProceed = async () => {
  if (!formData.authCode) {
    ElMessage({
      message: t('findPassword.messages.enterAuthCode'),
      grouping: true,
      type: 'success',
    });
    return;
  }
  const result = await Api.post(ApiUrls.CHECK_CODE, { email: formData.email, code: formData.authCode, type: 'FIND_PW' });
  token.value = result.data.token;

  // 인증번호 검증 완료
  currentStep.value = 2; // 비밀번호 재설정 단계로 이동
  clearInterval(state.timerId);
  state.isVerified = true;
  await nextTick(() => {
    newPassword.value.focus();
  })
};

// 2. 남은 시간을 'MM:SS' 형식으로 변환하는 computed 속성
const formattedTime = computed(() => {
  if (state.totalSeconds <= 0) {
    return '00:00';
  }
  const minutes = Math.floor(state.totalSeconds / 60);
  const seconds = state.totalSeconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
});

// 3. 타이머를 시작하는 함수
const startTimer = () => {
  // 이미 실행 중인 타이머가 있다면 초기화
  if (state.timerId) {
    clearInterval(state.timerId);
    state.isVerified = true;
  }

  // 타이머 초기 시간 설정
  state.totalSeconds = 180;

  state.timerId = setInterval(() => {
    state.totalSeconds -= 1; // 1초씩 감소

    // 시간이 다 되면 타이머를 멈추고 메시지 표시
    if (state.totalSeconds <= 0) {
      clearInterval(state.timerId as number);
      state.isVerified = true;
      state.timerId = null;
      ElMessage({
        type: 'error',
        message: t('findPassword.messages.authTimeExpired'),
      });
    }
  }, 1000);
};

/**
 * 비밀번호 재설정 함수
 * API를 호출하여 비밀번호를 변경한다.
 */
const resetPassword = async () => {
  // 비밀번호 유효성 검사
  if (!formData.newPassword || !formData.confirmPassword) {
    ElMessage.error(t('findPassword.messages.enterPasswords'));
    return;
  }
  if (formData.newPassword !== formData.confirmPassword) {
    ElMessage.error(t('findPassword.messages.passwordMismatch'));
    return;
  }

  try {

    await Dialogs.customConfirm(
        t('findPassword.dialogs.confirmChangeTitle'),
        t('findPassword.dialogs.confirmChangeMessage'),
        t('findPassword.dialogs.buttonOk'),
        t('findPassword.dialogs.buttonCancel'),
        '420px'
    );

    // 비밀번호 암호화
    const encryptedPassword = await Common.encryptPassword(formData.newPassword);

    // 비밀번호 변경 로직
    await Api.post(ApiUrls.UPDATE_PASSWORD, { token: token.value, password: encryptedPassword, userId: formData.userId });

    const loading = ElLoading.service({
      lock: true,
      text: 'Loading',
      background: 'rgba(0, 0, 0, 0.7)',
    })
    setTimeout(()=>{
      loading.close();
      ElMessage.success(t('findPassword.messages.passwordChanged'));
      currentStep.value = 3; // 완료 단계로 이동
    }, 1000);

  } catch (action) {
    // '취소' 또는 '닫기'를 눌렀을 때 실행되는 로직
    // ElMessageBox는 Promise를 반환하며, 취소 시 'cancel'을 reject합니다.
    if (action === 'cancel') {}
  }
};

/**
 * 로그인 페이지로 이동하는 함수
 */
const goToLogin = () => {
  router.push("/login");
};

/**
 * 아이디 찾기 페이지로 이동하는 함수
 */
const goToFindId = () => {
  // '/find-id' 경로가 실제 라우터에 설정되어 있어야 합니다.
  router.push("/FindId");
};

/**
 * 처음 단계(방법 선택)로 돌아가는 함수
 */
const goToFirstStep = () => {
  // 모든 상태를 초기화합니다.
  currentStep.value = 0;
  findMethod.value = null;
  Object.assign(formData, {
    userId: '',
    userName: '',
    email: '',
    authCode: '',
    newPassword: '',
    confirmPassword: '',
  });
  securityQuestion.value = '';
  securityAnswer.value = '';
  isCodeSent.value = false;
  showNoQuestionAlert.value = false;
  if (state.timerId) {
    clearInterval(state.timerId);
    state.timerId = null;
  }
};

/**
 * ✨ 보안 질문 인증 실패 시, 이메일 인증으로 전환하는 함수
 */
const switchToEmailMethod = () => {
  findMethod.value = 'email';
  showNoQuestionAlert.value = false; // 경고창 숨기기
  securityQuestion.value = '';
  securityAnswer.value = '';
};

/**
 * 이메일이 오지 않나요?
 */
const alertDescription = computed(() => t('findPassword.emailHelp.description'));
const checklist = computed(() => [
  { type: 'primary', icon: MoreFilled, text: t('findPassword.emailHelp.checkJunk') },
  { type: 'primary', icon: MoreFilled, text: t('findPassword.emailHelp.checkGmailTabs') },
  { type: 'primary', icon: MoreFilled, text: t('findPassword.emailHelp.checkEmailAddress', { email: formData.email || 'email@example.com' }) },
  { type: 'primary', icon: Promotion, text: t('findPassword.emailHelp.checkSenderAddress') }
]);
</script>

<template>
  <div class="find-password-container">
    <el-card class="find-password-card" shadow="never">

      <!-- Step 0: 방법 선택 -->
      <div v-if="currentStep === 0">
        <h2 class="title">{{ t('findPassword.title') }}</h2>
        <p class="description">{{ t('findPassword.description') }}</p>
        <div class="method-selection">
          <el-button
              type="primary"
              class="action-button"
              @click="selectMethod('email')"
          >
            {{ t('findPassword.methodEmail') }}
          </el-button>
          <el-button
              type="success"
              class="action-button"
              @click="selectMethod('security')"
          >
            {{ t('findPassword.methodSecurity') }}
          </el-button>
        </div>
      </div>


      <!-- Step 1: 본인 인증 (이메일) -->
      <div v-if="currentStep === 1 && findMethod === 'email'">
        <h2 class="title">{{ t('findPassword.step1.emailTitle') }}</h2>
        <p class="description">{{ t('findPassword.step1.emailDescription') }}</p>

        <el-form class="find-form" label-position="top" :model="formData">
          <el-form-item :label="t('findPassword.step1.labelUserId')">
            <el-input
                v-model="formData.userId"
                ref="userId"
                :placeholder="t('findPassword.step1.placeholderUserId')"
                size="large"
                :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item :label="t('findPassword.step1.labelName')">
            <el-input
                v-model="formData.userName"
                :placeholder="t('findPassword.step1.placeholderName')"
                size="large"
            />
          </el-form-item>
          <el-form-item :label="t('findPassword.step1.labelEmail')">
            <el-input
                v-model="formData.email"
                :placeholder="t('findPassword.step1.placeholderEmail')"
                size="large"
                class="input-with-button"
            >
              <template #append>
                <el-button
                    type="primary"
                    class="non-outline"
                    @click="sendAuthCode"
                    :disabled="isCodeSent"
                    :loading="emailLoading"
                >
                  {{ isCodeSent ? t('findPassword.step1.buttonResendCode') : t('findPassword.step1.buttonSendCode') }}
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item :label="t('findPassword.step1.labelAuthCode')">
            <el-input
                v-model="formData.authCode"
                :disabled="!isCodeSent"
                :placeholder="t('findPassword.step1.placeholderAuthCode')"
                size="large"
                :prefix-icon="Key"
            />
          </el-form-item>

          <!-- 타이머, 안내문구 -->
          <div class="timer-area">
            <el-text class="timer-text">
              <el-icon class="timer-icon"><Timer /></el-icon>
              {{ formattedTime }}
            </el-text>
            <div style="display: flex; align-items: center;">
              <el-text style="font-size: 12px;">{{ t('findPassword.step1.timerHelpText') }}</el-text>
              <el-popover
                  placement="right"
                  :width="600"
                  trigger="click">
                <template #reference>
                  <el-button :icon="QuestionFilled" type="info" link class="help-icon-button"/>
                </template>
                <div class="email-help-container">
                  <el-alert
                      :title="t('findPassword.emailHelp.title')"
                      :description="alertDescription"
                      type="info"
                      :closable="false"
                      show-icon
                      class="custom-alert"
                  />
                  <el-timeline style="margin-top: 20px;">
                    <el-timeline-item
                        v-for="(item, index) in checklist"
                        :key="index"
                        :type="item.type"
                        :icon="item.icon"
                        size="large"
                    >
                      <div v-html="item.text"></div>
                    </el-timeline-item>
                  </el-timeline>
                </div>
              </el-popover>
            </div>
          </div>
          <!-- 타이머, 안내문구 -->
        </el-form>

        <el-button type="primary" class="action-button" :disabled="!isCodeSent" @click="verifyAndProceed">
          {{ t('findPassword.step1.buttonConfirm') }}
        </el-button>
      </div>

      <!-- Step 1: 본인 인증 (보안 질문) -->
      <div v-if="currentStep === 1 && findMethod === 'security'">
        <h2 class="title">{{ t('findPassword.step1.securityTitle') }}</h2>
        <p class="description">{{ t('findPassword.step1.securityDescription') }}</p>

        <!-- 보안 질문이 없는 경우 표시되는 경고창 -->
        <el-alert
            v-if="showNoQuestionAlert"
            :title="t('findPassword.step1.alertNoQuestionTitle')"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 20px;"
        >
          <div class="alert-content">
            <span>{{ t('findPassword.step1.alertNoQuestionDesc') }}</span>
            <el-button link @click="switchToEmailMethod">
              <el-text style="font-weight: bold; margin-top: 4px;">
                <el-icon><Right /></el-icon>
                {{ t('findPassword.step1.switchToEmail') }}
              </el-text>
            </el-button>
          </div>
        </el-alert>

        <el-form class="find-form" label-position="top" :model="formData">
          <el-form-item :label="t('findPassword.step1.labelUserId')">
            <el-input
                v-model="formData.userId"
                ref="userId"
                :placeholder="t('findPassword.step1.placeholderUserId')"
                size="large"
                :prefix-icon="User"
                class="input-with-button"
            >
              <template #append>
                <el-button @click="fetchUserSecurityQuestion" :loading="securityQuestionLoading">
                  {{ t('findPassword.step1.buttonCheckQuestion') }}
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item :label="t('findPassword.step1.labelSecurityQuestion')">
            <el-input
                v-model="securityQuestion"
                :placeholder="t('findPassword.step1.placeholderSecurityQuestion')"
                size="large"
                readonly
                :prefix-icon="QuestionFilled"
            />
          </el-form-item>
          <el-form-item :label="t('findPassword.step1.labelAnswer')">
            <el-input
                v-model="securityAnswer"
                :placeholder="t('findPassword.step1.placeholderAnswer')"
                size="large"
                :disabled="!securityQuestion"
                @keyup.enter="verifySecurityAnswer"
            />
          </el-form-item>
        </el-form>

        <el-button
            type="primary"
            class="action-button"
            @click="verifySecurityAnswer"
            :disabled="!securityQuestion || !securityAnswer">
          {{ t('findPassword.step1.buttonConfirmAnswer') }}
        </el-button>
      </div>


      <!-- Step 2: 비밀번호 재설정 -->
      <div v-if="currentStep === 2">
        <h2 class="title">{{ t('findPassword.step2.title') }}</h2>
        <p class="description">{{ t('findPassword.step2.description') }}</p>

        <el-form class="find-form" label-position="top" :model="formData">
          <el-form-item :label="t('findPassword.step2.labelNewPassword')">
            <el-input
                v-model="formData.newPassword"
                ref="newPassword"
                type="password"
                :placeholder="t('findPassword.step2.placeholderNewPassword')"
                size="large"
                show-password
                :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item :label="t('findPassword.step2.labelConfirmPassword')">
            <el-input
                v-model="formData.confirmPassword"
                type="password"
                :placeholder="t('findPassword.step2.placeholderConfirmPassword')"
                size="large"
                show-password
                :prefix-icon="Lock"
            />
          </el-form-item>
        </el-form>

        <el-button type="primary" class="action-button" @click="resetPassword">
          {{ t('findPassword.step2.buttonReset') }}
        </el-button>
      </div>

      <!-- Step 3: 완료 -->
      <div v-if="currentStep === 3" class="result-section">
        <el-result
            icon="success"
            :title="t('findPassword.step3.title')">
          <template #sub-title>
            <p v-html="t('findPassword.step3.description')"></p>
          </template>
          <template #icon>
            <el-icon :size="64" color="var(--el-color-success)">
              <CircleCheckFilled />
            </el-icon>
          </template>
          <template #extra>
            <el-button type="primary" @click="goToLogin">
              {{ t('findPassword.step3.buttonLogin') }}
            </el-button>
          </template>
        </el-result>
      </div>

      <!-- 하단 공통 링크 -->
      <div v-if="currentStep < 3" class="navigation-links">
        <el-button type="info" link @click="goToLogin">{{ t('findPassword.nav.login') }}</el-button>
        <el-divider direction="vertical" />
        <el-button type="info" link @click="goToFindId">{{ t('findPassword.nav.findId') }}</el-button>
        <!-- '처음으로' 버튼은 인증 단계(step 1)에서만 표시 -->
        <template v-if="currentStep === 1">
          <el-divider direction="vertical" />
          <el-button type="info" link @click="goToFirstStep">{{ t('findPassword.nav.backToStart') }}</el-button>
        </template>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
/* FindId.vue의 스타일을 기반으로 일관성 유지 */
.find-password-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 100px);
}
.find-password-card {
  width: 450px;
  padding: 8px;
  box-sizing: border-box;
  border-radius: 12px;
}
.title {
  font-family: 'Poppins', sans-serif;
  font-weight: 700;
  letter-spacing: 1px;
  font-size: 26px;
  color: var(--el-color-primary);
  text-align: center;
  margin: 0 0 10px;
}
.description {
  font-size: 15px;
  color: #8492a6;
  text-align: center;
  margin-bottom: 30px;
}
/* 폼 스타일 */
.find-form { margin-top: 15px; }
.find-form .el-form-item { margin-bottom: 18px; }
.find-form :deep(.el-form-item__label) {
  font-size: 14px;
  color: var(--el-color-primary);
  padding-bottom: 6px;
  line-height: normal;
  width: 100%;
}
.input-with-button :deep(.el-input-group__append) {
  background-color: transparent;
  padding: 0;
}
.input-with-button :deep(.el-input-group__append .el-button) {
  border-radius: 0 var(--el-input-border-radius) var(--el-input-border-radius) 0;
  margin: -1px;
}
.non-outline {
  outline: 0;
  color: var(--el-color-info) !important;
}
.action-button {
  width: 100%;
  height: 48px;
  font-weight: bold;
  font-size: 16px;
  margin-top: 20px;
  color: var(--el-bg-color);
}
.result-section {
  text-align: center;
  padding: 20px 0;
}
.result-section .action-button {
  width: 60%;
  margin-top: 20px;
}
.navigation-links {
  margin-top: 25px;
  text-align: center;
}
.navigation-links .el-button {
  font-size: 14px;
}
.timer-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.timer-text {
  font-weight: bold;
  font-size: 12px;
  color: var(--el-color-primary);
}
.timer-icon {
  margin-right: 1px;
  vertical-align: middle;
}

/* 추가된 스타일 */
.method-selection {
  display: flex;
  flex-direction: column;
  margin-top: 20px;
}

.method-selection .action-button {
  margin-top: 0;
  margin-left: 0 !important;
}
.method-selection .action-button:first-child {
  margin-bottom: 15px;
}

</style>