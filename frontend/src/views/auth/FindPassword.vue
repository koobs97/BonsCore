<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { useRouter } from "vue-router";
import { ElAlert, ElMessage } from 'element-plus';
import {
  User,
  Lock,
  Key,
  CircleCheckFilled,
  QuestionFilled,
  Timer,
  MoreFilled,
  Promotion
} from '@element-plus/icons-vue';
import TheFooter from "@/components/layout/TheFooter.vue";

// Vue 라우터 인스턴스
const router = useRouter();

const state = reactive({
  totalSeconds: 180, // 전체 남은 시간을 초 단위로 관리
  timerId: null as any | null, // setInterval의 ID를 저장하기 위한 변수
})

// UI 흐름 제어를 위한 상태 변수
// 1: 본인 인증 단계, 2: 비밀번호 재설정 단계, 3: 완료 단계
const currentStep = ref(1);
const isCodeSent = ref(false); // 인증번호 전송 여부

// 폼 데이터 모델
const formData = reactive({
  userId: '',
  userName: '',
  email: '',
  authCode: '',
  newPassword: '',
  confirmPassword: '',
});

/**
 * (가상) 인증번호 전송 함수
 * 실제로는 API를 호출하여 인증번호를 발송합니다.
 */
const sendAuthCode = () => {
  // 간단한 유효성 검사
  if (!formData.userId || !formData.userName || !formData.email) {
    ElMessage.error('아이디, 이름, 이메일을 모두 입력해주세요.');
    return;
  }
  isCodeSent.value = true;
  ElMessage.success('인증번호가 이메일로 발송되었습니다.');

  startTimer();
};

/**
 * (가상) 인증번호 확인 및 다음 단계로 이동 함수
 * 실제로는 API를 호출하여 인증번호의 유효성을 검증합니다.
 */
const verifyAndProceed = () => {
  if (!formData.authCode) {
    ElMessage.error('인증번호를 입력해주세요.');
    return;
  }
  // 인증번호 검증 로직 (여기서는 성공으로 간주)
  console.log('인증번호 확인 완료');
  currentStep.value = 2; // 비밀번호 재설정 단계로 이동
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
  }

  // 타이머 초기 시간 설정
  state.totalSeconds = 180;

  state.timerId = setInterval(() => {
    state.totalSeconds -= 1; // 1초씩 감소

    // 시간이 다 되면 타이머를 멈추고 메시지 표시
    if (state.totalSeconds <= 0) {
      clearInterval(state.timerId as number);
      state.timerId = null;
      ElMessage({
        type: 'error',
        message: '인증시간이 초과되었습니다.',
      });
    }
  }, 1000);
};

/**
 * (가상) 비밀번호 재설정 함수
 * 실제로는 API를 호출하여 비밀번호를 변경합니다.
 */
const resetPassword = () => {
  // 비밀번호 유효성 검사
  if (!formData.newPassword || !formData.confirmPassword) {
    ElMessage.error('새 비밀번호와 확인 비밀번호를 모두 입력해주세요.');
    return;
  }
  if (formData.newPassword !== formData.confirmPassword) {
    ElMessage.error('비밀번호가 일치하지 않습니다.');
    return;
  }
  // 비밀번호 변경 로직 (여기서는 성공으로 간주)
  console.log('비밀번호 변경 완료');
  currentStep.value = 3; // 완료 단계로 이동
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
  router.push("/find-id");
};

/**
 * 이메일이 오지 않나요?
 */
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
</script>

<template>
  <div class="find-password-container">
    <el-card class="find-password-card" shadow="never">

      <!-- Step 1: 본인 인증 -->
      <div v-if="currentStep === 1">
        <h2 class="title">비밀번호 찾기</h2>
        <p class="description">가입 시 등록한 정보로 본인인증을 진행합니다.</p>

        <el-form class="find-form" label-position="top" :model="formData">
          <el-form-item label="아이디">
            <el-input
                v-model="formData.userId"
                placeholder="아이디를 입력하세요."
                size="large"
                :prefix-icon="User"
            />
          </el-form-item>
          <el-form-item label="이름">
            <el-input
                v-model="formData.userName"
                placeholder="가입 시 등록한 이름을 입력하세요."
                size="large"
            />
          </el-form-item>
          <el-form-item label="이메일">
            <el-input v-model="formData.email" placeholder="가입 시 등록한 이메일을 입력하세요." size="large" class="input-with-button">
              <template #append>
                <el-button type="primary" @click="sendAuthCode" :disabled="isCodeSent">
                  {{ isCodeSent ? '재전송' : '인증번호 전송' }}
                </el-button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="인증번호">
            <el-input
                v-model="formData.authCode"
                :disabled="!isCodeSent"
                placeholder="이메일로 수신된 인증번호를 입력하세요."
                size="large"
                :prefix-icon="Key"
            />
          </el-form-item>

          <div class="timer-area">

            <!-- 왼쪽 (타이머) -->
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
        </el-form>

        <el-button type="primary" class="action-button" :disabled="!isCodeSent" @click="verifyAndProceed">
          확인
        </el-button>
      </div>

      <!-- Step 2: 비밀번호 재설정 -->
      <div v-if="currentStep === 2">
        <h2 class="title">비밀번호 재설정</h2>
        <p class="description">새로 사용할 비밀번호를 입력해주세요.</p>

        <el-form class="find-form" label-position="top" :model="formData">
          <el-form-item label="새 비밀번호">
            <el-input
                v-model="formData.newPassword"
                type="password"
                placeholder="8자 이상, 영문/숫자/특수기호 조합"
                size="large"
                show-password
                :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item label="새 비밀번호 확인">
            <el-input
                v-model="formData.confirmPassword"
                type="password"
                placeholder="비밀번호를 다시 한번 입력해주세요."
                size="large"
                show-password
                :prefix-icon="Lock"
            />
          </el-form-item>
        </el-form>

        <el-button type="primary" class="action-button" @click="resetPassword">
          비밀번호 변경
        </el-button>
      </div>

      <!-- Step 3: 완료 -->
      <div v-if="currentStep === 3" class="result-section">
        <el-result
            icon="success"
            title="비밀번호 변경 완료"
            sub-title="성공적으로 비밀번호가 변경되었습니다. 다시 로그인해주세요."
        >
          <template #icon>
            <el-icon :size="64" color="var(--el-color-success)">
              <CircleCheckFilled />
            </el-icon>
          </template>
          <template #extra>
            <el-button type="primary" @click="goToLogin">
              로그인 하기
            </el-button>
          </template>
        </el-result>
      </div>

      <!-- 하단 공통 링크 -->
      <div v-if="currentStep < 3" class="navigation-links">
        <el-button type="info" link @click="goToLogin">로그인</el-button>
        <el-divider direction="vertical" />
        <el-button type="info" link @click="goToFindId">아이디 찾기</el-button>
      </div>
    </el-card>

    <TheFooter />
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
}
.title {
  font-size: 26px;
  color: #1f2d3d;
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
.find-form .el-form-item { margin-bottom: 8px; }
.find-form :deep(.el-form-item__label) {
  font-size: 14px;
  color: #475669;
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
.action-button {
  width: 100%;
  height: 48px;
  font-weight: bold;
  font-size: 16px;
  margin-top: 20px;
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
  color: #1f2d3d;
}
.timer-icon {
  margin-right: 1px;
  vertical-align: middle;
}
</style>