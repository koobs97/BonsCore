<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue';
import { onBeforeRouteLeave, useRouter } from 'vue-router';
import { ElAlert, ElMessage } from 'element-plus';
import { MoreFilled, Promotion, QuestionFilled, Timer } from "@element-plus/icons-vue";
import { ApiUrls } from "@/api/apiUrls";
import { Api } from "@/api/axiosInstance";
import { useI18n } from "vue-i18n";

// i18n
const { t } = useI18n();

const router = useRouter();

// 본인인증 관련 상태 변수 (예시)
const userName = ref('');
const email = ref('');
const verificationCode = ref('');
const isCodeSent = ref(false);
const isVerifying = ref(false);
const isCardLoading = ref(false);
const isSendingCode = ref(false);

const verificationType = ref('');

onMounted(() => {
  let type = history.state.type;

  if (!type) {
    type = sessionStorage.getItem('verificationType');
  }

  sessionStorage.removeItem('verificationType');

  // type 값이 유효한지('DORMANT' 또는 'ABNORMAL') 확인
  if (type === 'DORMANT' || type === 'ABNORMAL') {
    verificationType.value = type;
  } else {
    // 유효한 type이 없으면 비정상적인 접근으로 간주하고 로그인 페이지로 리다이렉트
    ElMessage.error(t('verifyIdentity.messages.invalidAccess'));
    router.replace('/login');
  }
});

const pageTitle = computed(() => {
  return verificationType.value ? t(`verifyIdentity.title.${verificationType.value}`) : '';
});

const pageDescription = computed(() => {
  return verificationType.value ? t(`verifyIdentity.description.${verificationType.value}`) : '';
});

const submitButtonText = computed(() => {
  return verificationType.value ? t(`verifyIdentity.buttons.submit.${verificationType.value}`) : '';
});

// 타이머 상태관리
const state = reactive({
  totalSeconds: 180, // 전체 남은 시간을 초 단위로 관리
  timerId: null as any | null, // setInterval의 ID를 저장하기 위한 변수
  isVerified: false,
})

// 1. 남은 시간을 'MM:SS' 형식으로 변환하는 computed 속성
const formattedTime = computed(() => {
  if (state.totalSeconds <= 0) {
    return '00:00';
  }
  const minutes = Math.floor(state.totalSeconds / 60);
  const seconds = state.totalSeconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
});

// 2. 타이머를 시작하는 함수
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
      state.timerId = null;
      state.isVerified = true;
      ElMessage({ message: t('verifyIdentity.messages.authTimeExpired'), type: 'error' });
    }
  }, 1000);
};

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

// 메일 안내 창 관련
const alertDescription = computed(() => t('verifyIdentity.emailHelp.description'));
const checklist = computed(() => [
  { type: 'primary', icon: MoreFilled, text: t('verifyIdentity.emailHelp.checkJunk') },
  { type: 'primary', icon: MoreFilled, text: t('verifyIdentity.emailHelp.checkGmailTabs') },
  { type: 'primary', icon: MoreFilled, text: t('verifyIdentity.emailHelp.checkEmailAddress', { email: email.value || 'email@example.com' }) },
  { type: 'primary', icon: Promotion, text: t('verifyIdentity.emailHelp.checkSenderAddress') }
]);

/**
 * 인증번호 발송 (API 호출)
 */
const sendVerificationCode = async () => {
  if (!userName.value || !email.value) {
    ElMessage({
      message: t('verifyIdentity.messages.enterNameAndEmail'),
      grouping: true,
      type: 'error',
    });
    return;
  }

  isSendingCode.value = true;
  try {

    await Api.post(ApiUrls.SEND_MAIL, {
      userName: userName.value,
      email: email.value,
      type: verificationType.value // <-- 'DORMANT' 또는 'ABNORMAL'
    });

    ElMessage({ message: t('verifyIdentity.messages.emailSent'), grouping: true, type: 'success' });
    isCodeSent.value = true;
    startTimer();

  } catch (error) { } finally {
    isSendingCode.value = false;
  }

};

/**
 * 인증번호 확인 및 계정 활성화 (API 호출)
 */
const verifyAndActivate = async () => {

  if (!verificationCode.value) {
    ElMessage.error(t('verifyIdentity.messages.enterAuthCode'));
    return;
  }


  try {
    const result = await Api.post(ApiUrls.CHECK_CODE, { email: email.value, code: verificationCode.value, type: verificationType.value });

    // 휴먼계정인 경우 테이블 데이터 원복
    if(verificationType.value === 'DORMANT') {
      ElMessage.info(t('verifyIdentity.messages.verificationSuccessDormant'));
      await Api.post(ApiUrls.ACTIVATE_DORMANT, { email: email.value }, // body 데이터
          true,
          { // Axios 요청 설정 객체
            headers: {
              'Authorization': `Bearer ${result.data.token}`
            }
          });
    }
    else {
      ElMessage.success(t('verifyIdentity.messages.verificationSuccessAbnormal'));
    }

    isCardLoading.value = true;

    // 타이머 초기화
    clearInterval(state.timerId);
    state.isVerified = true;

    // 인증 완료 후 로그인 페이지로 이동
    setTimeout(async ()=>{
      await router.push('/login');
    }, 1500);

  } catch (error) {
    ElMessage.error(t('verifyIdentity.messages.verificationFailed'));
  } finally {
    isVerifying.value = false;
  }
};

/**
 * 로그인 페이지로 돌아가기
 */
const goToLogin = () => {
  router.push('/login');
}
</script>

<template>
  <div class="verify-container">
    <el-card
        class="verify-card"
        shadow="never"
        v-loading="isCardLoading"
        :element-loading-text="t('verifyIdentity.loadingText')"
    >
      <h2 class="verify-title">{{ pageTitle }}</h2>
      <p class="verify-description">{{ pageDescription }}</p>

      <el-form class="verify-form" @submit.prevent>
        <el-input
            v-model="userName"
            :placeholder="t('verifyIdentity.placeholders.name')"
            class="verify-input"
            :disabled="isCodeSent"
        />
        <el-input
            v-model="email"
            :placeholder="t('verifyIdentity.placeholders.email')"
            class="verify-input"
            :disabled="isCodeSent"
        />

        <el-button
            type="primary"
            class="verify-button"
            @click="sendVerificationCode"
            v-if="!isCodeSent"
            :loading="isSendingCode"
        >
          {{ t('verifyIdentity.buttons.sendCode') }}
        </el-button>

        <template v-if="isCodeSent">
          <el-input
              v-model="verificationCode"
              :placeholder="t('verifyIdentity.placeholders.authCode')"
              class="verify-input"
          />
          <div class="timer-area">
            <el-text class="timer-text">
              <el-icon class="timer-icon"><Timer /></el-icon>
              {{ formattedTime }}
            </el-text>

            <!-- 오른쪽 ("인증번호가 오지 않나요?" 관련 부분) -->
            <div style="display: flex; align-items: center;">
              <el-text style="font-size: 12px;">{{ t('verifyIdentity.timerHelpText') }}</el-text>
              <el-popover placement="right" :width="600" trigger="click">
                <template #reference>
                  <el-button :icon="QuestionFilled" type="info" link class="help-icon-button"/>
                </template>
                <div class="email-help-container">
                  <el-alert
                      :title="t('verifyIdentity.emailHelp.title')"
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
          <el-button
              type="primary"
              class="verify-button"
              :loading="isVerifying"
              @click="verifyAndActivate"
          >
            {{ submitButtonText }}
          </el-button>
        </template>

        <div class="back-to-login">
          <el-button type="info" link @click="goToLogin">
            {{ t('verifyIdentity.buttons.backToLogin') }}
          </el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.verify-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 100px);
}
.verify-card {
  width: 430px;
  padding: 8px;
}
.verify-title {
  font-size: 24px;
  font-weight: 700;
  text-align: center;
  color: var(--el-color-primary);
  margin-bottom: 12px;
}
.verify-description {
  text-align: center;
  color: var(--el-text-color-secondary);
  margin-bottom: 30px;
}
.verify-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.verify-input :deep(.el-input__inner) {
  height: 45px;
}

.verify-button {
  height: 48px;
  font-size: 16px;
  font-weight: bold;
}
.back-to-login {
  text-align: center;
  margin-top: 20px;
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
</style>