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
import { ref, reactive, computed, onMounted, nextTick, h } from 'vue';
import { onBeforeRouteLeave, useRouter } from 'vue-router';
import { ElAlert, ElLoading, ElMessage, ElMessageBox } from 'element-plus';
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
import {Api} from "@/api/axiosInstance";
import {ApiUrls} from "@/api/apiUrls";
import SignUpConfirm from "@/components/MessageBox/SignUpConfirm.vue";
import { Common } from '@/common/common';

// Vue 라우터 인스턴스
const router = useRouter();

// 타이머 상태
const state = reactive({
  totalSeconds: 180, // 전체 남은 시간을 초 단위로 관리
  timerId: null as any | null, // setInterval의 ID를 저장하기 위한 변수
  isVerified: false,
})

// UI 흐름 제어를 위한 상태 변수
// 1: 본인 인증 단계, 2: 비밀번호 재설정 단계, 3: 완료 단계
const currentStep = ref(1);
const isCodeSent = ref(false); // 인증번호 전송 여부

// 버튼 loading
const emailLoading = ref(false);

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

// 화면진입 시
onMounted(() => {
  userId.value.focus();
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
 * 인증번호 전송 함수
 * 실제로는 API를 호출하여 인증번호를 발송합니다.
 */
const sendAuthCode = async () => {

  // 간단한 유효성 검사
  if (!formData.userId || !formData.userName || !formData.email) {
    ElMessage.error('아이디, 이름, 이메일을 모두 입력해주세요.');
    return;
  }

  try {
    emailLoading.value = true;
    await Api.post(ApiUrls.SEND_MAIL, { nonMaskedId: formData.userId, userName: formData.userName, email: formData.email, type: 'PW' });
    ElMessage({
      message: '이메일이 전송되었습니다.',
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
 * 실제로는 API를 호출하여 인증번호의 유효성을 검증합니다.
 */
const verifyAndProceed = async () => {
  if (!formData.authCode) {
    ElMessage.error('인증번호를 입력해주세요.');
    return;
  }
  const result = await Api.post(ApiUrls.CHECK_CODE, { email: formData.email, code: formData.authCode });
  token.value = result.data.token;

  // 인증번호 검증 완료
  currentStep.value = 2; // 비밀번호 재설정 단계로 이동
  clearInterval(state.timerId);
  state.isVerified = true;
  nextTick(() => {
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
        message: '인증시간이 초과되었습니다.',
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
    ElMessage.error('새 비밀번호와 확인 비밀번호를 모두 입력해주세요.');
    return;
  }
  if (formData.newPassword !== formData.confirmPassword) {
    ElMessage.error('비밀번호가 일치하지 않습니다.');
    return;
  }

  try {
    await ElMessageBox.confirm(
        // 1. message 옵션에 h() 함수를 사용하여 커스텀 컴포넌트를 렌더링합니다.
        h(SignUpConfirm, {
          // CustomConfirm 컴포넌트에 props 전달
          title: '비밀번호 변경',
          message: '비밀번호를 변경하시겠습니까?',
        }),
        // 2. 기본 title은 사용하지 않으므로 빈 문자열로 둡니다.
        '',
        {
          confirmButtonText: '확인',
          cancelButtonText: '취소',
          // 3. 커스텀 클래스를 추가하여 세부 스타일을 조정할 수 있습니다.
          customClass: 'custom-message-box',
          // 4. CustomConfirm 컴포넌트가 자체 아이콘과 UI를 가지므로,
          //    MessageBox의 기본 UI 요소들은 비활성화합니다.
          showClose: false, // 오른쪽 위 'X' 닫기 버튼 숨김
          type: '', // 기본 'warning' 아이콘 숨김
        }
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
      ElMessage.success('비밀번호가 변경되었습니다.');
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
                ref="userId"
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
            <el-input
                v-model="formData.email"
                placeholder="가입 시 등록한 이메일을 입력하세요."
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

          <!-- 타이머, 안내문구 -->
          <div class="timer-area">
            <el-text class="timer-text">
              <el-icon class="timer-icon"><Timer /></el-icon>
              {{ formattedTime }}
            </el-text>
            <div style="display: flex; align-items: center;">
              <el-text style="font-size: 12px;">인증번호가 오지 않나요?</el-text>
              <el-popover
                  placement="right"
                  :width="600"
                  trigger="click">
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
          <!-- 타이머, 안내문구 -->
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
                ref="newPassword"
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
        >
          <template #sub-title>
            <p v-html="'성공적으로 비밀번호가 변경되었습니다. <br>다시 로그인해주세요.'"></p>
          </template>
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
.non-outline {
  outline: 0;
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