<script setup lang="ts">
/**
 * ========================================
 * 파일명   : ChangePassword.vue
 * ----------------------------------------
 * 설명     : 비밀번호 변경 (중복 로그인 등 비로그인 상태에서 변경)
 * 작성자   : koobonsang
 * 버전     : 1.0
 * 작성일자 : 2025-11-14
 * ========================================
 */
import { ref, reactive, computed, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElLoading } from 'element-plus';
import { User, Key, Lock, Timer, QuestionFilled, Promotion, MoreFilled } from '@element-plus/icons-vue';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { Common } from '@/common/common';
import { Dialogs } from "@/common/dialogs";
import { useI18n } from "vue-i18n";

// i18n
const { t } = useI18n();
const router = useRouter();

// UI 흐름 제어 (0: 본인 인증, 1: 비밀번호 재설정, 2: 완료)
const currentStep = ref(0);
const isCodeSent = ref(false);
const emailLoading = ref(false);
const token = ref('');

// 폼 데이터
const formData = reactive({
  userId: '',
  userName: '',
  email: '',
  authCode: '',
  newPassword: '',
  confirmPassword: '',
});

// 타이머 상태
const timerState = reactive({
  totalSeconds: 180,
  timerId: null as any | null,
});

// 타이머 시간 형식 변환
const formattedTime = computed(() => {
  if (timerState.totalSeconds <= 0) return '00:00';
  const minutes = Math.floor(timerState.totalSeconds / 60);
  const seconds = timerState.totalSeconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
});

// 타이머 시작
const startTimer = () => {
  stopTimer();
  timerState.totalSeconds = 180;
  timerState.timerId = setInterval(() => {
    timerState.totalSeconds--;
    if (timerState.totalSeconds <= 0) {
      stopTimer();
      ElMessage.error(t('findPassword.messages.authTimeExpired'));
    }
  }, 1000);
};

// 타이머 중지
const stopTimer = () => {
  if (timerState.timerId) {
    clearInterval(timerState.timerId);
    timerState.timerId = null;
  }
};

// 컴포넌트가 사라지기 전에 타이머 정리
onBeforeUnmount(() => {
  stopTimer();
});

// 인증번호 전송
const sendAuthCode = async () => {
  if (!formData.userId || !formData.userName || !formData.email) {
    ElMessage.error(t('findPassword.messages.enterAllFields'));
    return;
  }
  emailLoading.value = true;
  try {
    // 비밀번호 찾기(PW)와 동일한 타입의 메일 전송 API 사용"PW"
    await Api.post(ApiUrls.SEND_MAIL, { nonMaskedId: formData.userId, userName: formData.userName, email: formData.email, type: 'PW' });
    ElMessage.success(t('findPassword.messages.emailSent'));
    isCodeSent.value = true;
    startTimer();
  } finally {
    emailLoading.value = false;
  }
};

// 인증번호 확인
const verifyCode = async () => {
  if (!formData.authCode) {
    ElMessage.error(t('findPassword.messages.enterAuthCode'));
    return;
  }
  try {
    const result = await Api.post(ApiUrls.CHECK_CODE, { email: formData.email, code: formData.authCode, type: 'PW' });
    token.value = result.data.token;
    stopTimer();
    currentStep.value = 1; // 비밀번호 재설정 단계로 이동
    ElMessage.success(t('findPassword.messages.verificationComplete'));
  } catch (e) {
    // API에서 던지는 에러 메시지를 그대로 사용
  }
};

// 비밀번호 재설정
const resetPassword = async () => {
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

    const encryptedPassword = await Common.encryptPassword(formData.newPassword);
    await Api.post(ApiUrls.UPDATE_PASSWORD, { token: token.value, password: encryptedPassword, userId: formData.userId });

    const loading = ElLoading.service({ lock: true, text: 'Loading' });
    setTimeout(() => {
      loading.close();
      ElMessage.success(t('findPassword.messages.passwordChanged'));
      currentStep.value = 2; // 완료 단계로 이동
    }, 1000);
  } catch (action) {
    if (action === 'cancel') { /* 취소 시 아무것도 안 함 */ }
  }
};

const goToLogin = () => router.push("/login");

// 이메일 도움말 팝오버 관련 데이터
const alertDescription = computed(() => t('findPassword.emailHelp.description'));
const checklist = computed(() => [
  { type: 'primary', icon: MoreFilled, text: t('findPassword.emailHelp.checkJunk') },
  { type: 'primary', icon: MoreFilled, text: t('findPassword.emailHelp.checkGmailTabs') },
  { type: 'primary', icon: MoreFilled, text: t('findPassword.emailHelp.checkEmailAddress', { email: formData.email || 'email@example.com' }) },
  { type: 'primary', icon: Promotion, text: t('findPassword.emailHelp.checkSenderAddress') }
]);

</script>

<template>
  <div class="change-password-container">
    <el-card class="change-password-card" shadow="never">

      <!-- Step 0: 본인 인증 -->
      <div v-if="currentStep === 0">
        <h2 class="title">{{ t('findPassword.step2.title') }}</h2> <!-- 비밀번호 재설정 -->
        <p class="description">{{ t('findPassword.step1.emailDescription') }}</p> <!-- 가입 시 등록한 정보로 본인인증을 진행 -->

        <el-form class="change-form" label-position="top">
          <el-form-item :label="t('findPassword.step1.labelUserId')">
            <el-input
                v-model="formData.userId"
                :placeholder="t('findPassword.step1.placeholderUserId')"
                size="large"
                :prefix-icon="User" />
          </el-form-item>
          <el-form-item :label="t('findPassword.step1.labelName')">
            <el-input
                v-model="formData.userName"
                :placeholder="t('findPassword.step1.placeholderName')"
                size="large" />
          </el-form-item>
          <el-form-item :label="t('findPassword.step1.labelEmail')">
            <el-input
                v-model="formData.email"
                :placeholder="t('findPassword.step1.placeholderEmail')"
                size="large"
                class="input-with-button">
              <template #append>
                <el-button
                    @click="sendAuthCode"
                    :disabled="isCodeSent && timerState.timerId"
                    :loading="emailLoading">
                  {{
                    (isCodeSent && timerState.timerId)
                    ? t('findPassword.step1.buttonResendCode')
                    : t('findPassword.step1.buttonSendCode')
                  }}
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
                @keyup.enter="verifyCode"
            />
          </el-form-item>

          <div class="timer-area" v-if="isCodeSent">
            <el-text class="timer-text">
              <el-icon class="timer-icon">
                <Timer />
              </el-icon>
              {{ formattedTime }}
            </el-text>
            <div>
              <el-text style="font-size: 12px;">{{ t('findPassword.step1.timerHelpText') }}</el-text>
              <el-popover placement="right" :width="600" trigger="click">
                <template #reference><el-button :icon="QuestionFilled" type="info" link class="help-icon-button"/></template>
                <div class="email-help-container">
                  <el-alert
                      :title="t('findPassword.emailHelp.title')"
                      :description="alertDescription"
                      type="info"
                      :closable="false"
                      show-icon
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
        </el-form>
        <el-button
            type="primary"
            class="action-button"
            :disabled="!isCodeSent"
            @click="verifyCode">
          {{ t('findPassword.step1.buttonConfirm') }}
        </el-button>
      </div>

      <!-- Step 1: 비밀번호 재설정 -->
      <div v-if="currentStep === 1">
        <h2 class="title">
          {{ t('findPassword.step2.title') }}
        </h2>
        <p class="description">
          {{ t('findPassword.step2.description') }}
        </p>
        <el-form class="change-form" label-position="top">
          <el-form-item :label="t('findPassword.step2.labelNewPassword')">
            <el-input
                v-model="formData.newPassword"
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
                @keyup.enter="resetPassword"
            />
          </el-form-item>
        </el-form>
        <el-button
            type="primary"
            class="action-button"
            @click="resetPassword">
          {{ t('findPassword.step2.buttonReset') }}
        </el-button>
      </div>

      <!-- Step 2: 완료 -->
      <div v-if="currentStep === 2" class="result-section">
        <el-result
            icon="success"
            :title="t('findPassword.step3.title')">
          <template #sub-title>
            <p v-html="t('findPassword.step3.description')"></p>
          </template>
          <template #extra>
            <el-button
                type="primary"
                @click="goToLogin">
              {{ t('findPassword.step3.buttonLogin') }}
            </el-button>
          </template>
        </el-result>
      </div>

      <div v-if="currentStep !== 2" class="navigation-links">
        <el-button
            type="info"
            link
            @click="goToLogin">
          {{ t('findPassword.nav.login') }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
/* FindPassword.vue와 스타일 일관성을 위해 클래스명 유지 */
.change-password-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 100px);
}
.change-password-card {
  width: 450px;
  padding: 8px 20px 20px;
  border-radius: 12px;
}
.title {
  font-size: 26px;
  font-weight: 700;
  text-align: center;
  margin: 20px 0 10px;
}
.description {
  font-size: 15px;
  color: #8492a6;
  text-align: center;
  margin-bottom: 30px;
}
.change-form .el-form-item {
  margin-bottom: 18px;
}
.action-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  margin-top: 10px;
}
.navigation-links {
  margin-top: 25px;
  text-align: center;
}
.timer-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: -10px;
  margin-bottom: 20px;
}
.timer-text {
  font-weight: bold;
  font-size: 13px;
  color: var(--el-color-primary);
}
.timer-icon {
  margin-right: 4px;
  vertical-align: middle;
}
.input-with-button :deep(.el-input-group__append) {
  padding: 0;
}
.input-with-button :deep(.el-input-group__append .el-button) {
  border-radius: 0 var(--el-input-border-radius) var(--el-input-border-radius) 0;
  margin: -1px;
}
</style>