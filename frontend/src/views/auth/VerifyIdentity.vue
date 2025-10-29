<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { onBeforeRouteLeave, useRouter, useRoute } from 'vue-router';
import { ElAlert, ElMessage } from 'element-plus';
import { MoreFilled, Promotion, QuestionFilled, Timer } from "@element-plus/icons-vue";
import { ApiUrls } from "@/api/apiUrls";
import { Api } from "@/api/axiosInstance";

const router = useRouter();
const route = useRoute();

// 본인인증 관련 상태 변수 (예시)
const userName = ref('');
const email = ref('');
const verificationCode = ref('');
const isCodeSent = ref(false);
const isVerifying = ref(false);
const isCardLoading = ref(false);

const verifyType = computed(() => {
  return route.query.type === 'ABNORMAL' ? 'ABNORMAL' : 'DORMANT';
});

const pageTitle = computed(() => {
  return verifyType.value === 'ABNORMAL' ? '비정상 로그인 인증' : '휴면 계정 활성화';
});

const pageDescription = computed(() => {
  return verifyType.value === 'ABNORMAL'
      ? '회원님의 계정 보호를 위해 본인인증이 필요합니다.'
      : '본인인증을 통해 안전하게 계정을 다시 활성화하세요.';
});

const submitButtonText = computed(() => {
  return verifyType.value === 'ABNORMAL' ? '인증 확인' : '확인 및 계정 활성화';
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
      ElMessage({ message: '인증시간이 초과되었습니다.', type: 'error' });
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
const alertDescription = ref('메일 서버 상황에 따라 최대 5분까지 지연될 수 있습니다.\n5분 후에도 메일이 없다면 아래 내용을 확인해주세요.');
const checklist = ref([
  {
    type: 'primary',
    icon: MoreFilled,
    text: `<b>스팸(Junk) 메일함</b>을 가장 먼저 확인해주세요.`
  },
  {
    type: 'primary',
    icon: MoreFilled,
    text: `<b>[Gmail]</b>의 경우, <b>'프로모션'</b> 또는 <b>'소셜'</b> 탭으로 분류될 수 있습니다.`
  },
  {
    type: 'primary',
    icon: MoreFilled,
    text: `입력하신 이메일 주소: <b>email@example.com</b><br>이메일 주소가 정확한지 확인해주세요.`
  },
  {
    type: 'primary',
    icon: Promotion,
    text: `발신자 주소: <b>koobs970729@gmail.com</b><br>주소록에 추가하면 다음부터 메일을 안정적으로 받을 수 있습니다.`
  }
]);

/**
 * 인증번호 발송 (API 호출)
 */
const sendVerificationCode = async () => {
  if (!userName.value || !email.value) {
    ElMessage({
      message: '이름과 이메일을 모두 입력해주세요.',
      grouping: true,
      type: 'error',
    });
    return;
  }

  await Api.post(ApiUrls.SEND_MAIL, { userName: userName.value, email: email.value, type: 'LOCKED' });
  ElMessage({ message: '이메일이 전송되었습니다.', grouping: true, type: 'success' });

  isCodeSent.value = true;
  ElMessage.success('인증번호가 발송되었습니다. 이메일을 확인해주세요.');
  startTimer();

};

/**
 * 인증번호 확인 및 계정 활성화 (API 호출)
 */
const verifyAndActivate = async () => {

  if (!verificationCode.value) {
    ElMessage.error('인증번호를 입력해주세요.');
    return;
  }

  isVerifying.value = true;
  try {
    const result = await Api.post(ApiUrls.CHECK_CODE, { email: email.value, code: verificationCode.value });
    ElMessage.success('본인인증이 완료되었습니다. 계정이 활성화되었습니다.');

    isCardLoading.value = true;

    // 타이머 초기화
    clearInterval(state.timerId);
    state.isVerified = true;

    // 인증 완료 후 로그인 페이지로 이동
    setTimeout(async ()=>{
      await router.push('/login');
    }, 1500);

  } catch (error) {
    ElMessage.error('인증에 실패했습니다. 다시 시도해주세요.');
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
        element-loading-text="잠시 후 로그인 페이지로 이동합니다..."
    >
      <h2 class="verify-title">{{ pageTitle }}</h2>
      <p class="verify-description">{{ pageDescription }}</p>

      <el-form class="verify-form" @submit.prevent>
        <el-input
            v-model="userName"
            placeholder="이름"
            class="verify-input"
            :disabled="isCodeSent"
        />
        <el-input
            v-model="email"
            placeholder="이메일"
            class="verify-input"
            :disabled="isCodeSent"
        />

        <el-button
            type="primary"
            class="verify-button"
            @click="sendVerificationCode"
            v-if="!isCodeSent"
        >
          인증번호 발송
        </el-button>

        <template v-if="isCodeSent">
          <el-input
              v-model="verificationCode"
              placeholder="인증번호 6자리"
              class="verify-input"
          />
          <div class="timer-area">
            <el-text class="timer-text">
              <el-icon class="timer-icon"><Timer /></el-icon>
              {{ formattedTime }}
            </el-text>

            <!-- 오른쪽 ("인증번호가 오지 않나요?" 관련 부분) -->
            <div style="display: flex; align-items: center;">
              <el-text style="font-size: 12px;">인증번호가 오지 않나요?</el-text>
              <el-popover placement="right" :width="600" trigger="click">
                <template #reference>
                  <el-button :icon="QuestionFilled" type="info" link class="help-icon-button"/>
                </template>
                <div class="email-help-container">
                  <el-alert
                      title="이메일이 도착하지 않았나요?"
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
          <el-button type="info" link @click="goToLogin">로그인 페이지로 돌아가기</el-button>
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