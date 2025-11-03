<script setup lang="ts">
/**
 * ========================================
 * 파일명   : FindId.vue
 * ----------------------------------------
 * 설명     : 아이디 찾기
 * 작성자   : koobonsang
 * 버전     : 1.0
 * 작성일자 : 2025-07-20
 * ========================================
 */
import { computed, onMounted, reactive, ref } from 'vue';
import { DocumentCopy, Key, MoreFilled, Promotion, QuestionFilled, Timer, InfoFilled } from '@element-plus/icons-vue';
import { ElAlert, ElMessage} from 'element-plus';
import { onBeforeRouteLeave, useRouter } from 'vue-router';
import { ApiUrls } from "@/api/apiUrls";
import { Api } from "@/api/axiosInstance";
import { Common } from "@/common/common";

// router
const router = useRouter();

// 탭 상태 관리를 위한 ref (UI 표시용)
const activeTab = ref('email');

// 타이머 상태관리
const state = reactive({
  totalSeconds: 180, // 전체 남은 시간을 초 단위로 관리
  timerId: null as any | null, // setInterval의 ID를 저장하기 위한 변수
  isVerified: false,
})

// UI 흐름 제어를 위한 상태
const isCodeSent = ref(false); // 인증번호가 전송되었는지 여부
const isIdFound = ref(false);  // 아이디를 찾았는지 여부

// 폼 데이터 (UI 표시용)
const userName = ref('');
const userEmail = ref('');
const authCode = ref('');

// focus
const userNameRef = ref();
const emailRef = ref();

// 버튼 loading
const emailLoading = ref(false);

// 찾은 아이디 정보 (화면 표시용과 실제 데이터 분리)
const maskedFoundUserId = ref('example***'); // 화면에 표시될 마스킹된 아이디
const fullFoundUserId = ref('example_full_id'); // 실제 API 응답으로 받을 전체 아이디

// 화면진입 시
onMounted(() => {
  userNameRef.value.focus();
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
 */
const sendAuthCode = async () => {

  // 필수입력 체크
  if(Common.isEmpty(userName.value)) {
    ElMessage({ message: '이름을 입력하세요.', grouping: true, type: 'error' });
    userNameRef.value.focus();
    return;
  }
  if(Common.isEmpty(userEmail.value)) {
    ElMessage({ message: '이메일을 입력하세요.', grouping: true, type: 'error' });
    emailRef.value.focus();
    return;
  }

  // 인증번호 전송
  try {
    emailLoading.value = true;
    await Api.post(ApiUrls.SEND_MAIL, { userName: userName.value, email: userEmail.value, type: 'ID' });
    ElMessage({ message: '이메일이 전송되었습니다.', grouping: true, type: 'success' });
  } finally {
    emailLoading.value = false;
  }

  // 인증번호 입력란 비활성화 해제
  isCodeSent.value = true;

  // 타이머 시작
  state.isVerified = false;
  startTimer();
};

/**
 * 아이디 찾기 확인 함수
 */
const findId = async () => {

  const result = await Api.post(ApiUrls.CHECK_CODE, { email: userEmail.value, code: authCode.value, type: 'FIND_ID' });
  maskedFoundUserId.value = result.data.userId;

  // 아이디 보여주기 영역 open
  isIdFound.value = true;

  // 타이머 초기화
  clearInterval(state.timerId);
  state.isVerified = true;
}

/**
 * 클립보드 복사 기능
 * @param {string} text - 복사할 텍스트
 */
const copyToClipboard = async (text: string) => {
  try {
    const response = await Api.post(ApiUrls.COPY_ID, { email: userEmail.value })
    await navigator.clipboard.writeText(response);
    ElMessage({ message: '아이디가 복사되었습니다.', grouping: true, type: 'success' });
  } catch (err) {
    ElMessage({ message: '복사에 실패했습니다. 다시 시도해주세요.', grouping: true, type: 'error' });
    console.error('Failed to copy ID: ', err);
  }
};

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
 * 화면이동
 * @param param
 */
const onClickToGoPage = (param: string) => {
  router.push("/" + param);
}

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
</script>

<template>
  <div class="find-id-container">
    <el-card class="find-id-card" shadow="never">

      <!-- 1단계: 아이디 찾기 정보 입력 -->
      <div v-if="!isIdFound">
        <h2 class="title">아이디 찾기</h2>
        <p class="description">가입 시 등록한 정보로 아이디를 찾을 수 있습니다.</p>

        <el-tabs v-model="activeTab" class="find-tabs" stretch>
          <!-- 이메일 인증 -->
          <el-tab-pane label="이메일로 찾기" name="email">
            <el-form
                class="find-form"
                label-position="top"
            >
              <el-form-item label="이름">
                <el-input
                    v-model="userName"
                    ref="userNameRef"
                    placeholder="가입 시 등록한 이름을 입력하세요."
                    size="large"
                />
              </el-form-item>
              <el-form-item label="이메일">
                <el-input
                    v-model="userEmail"
                    ref="emailRef"
                    placeholder="가입 시 등록한 이메일을 입력하세요."
                    size="large"
                    class="input-with-button"
                >
                  <template #append>
                    <el-button
                        class="non-outline"
                        type="primary"
                        @click="sendAuthCode"
                        :loading="emailLoading"
                    >
                      {{ isCodeSent ? '재전송' : '인증번호 전송' }}
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>

              <!-- 인증번호 입력 필드는 전송 후에만 활성화되며 표시됩니다. -->
              <el-form-item label="인증번호" style="margin-bottom: 4px;">
                <el-input
                    v-model="authCode"
                    :disabled="!isCodeSent"
                    placeholder="수신된 인증번호를 입력하세요."
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
              <!-- 타이머, 안내문구 -->

            </el-form>
          </el-tab-pane>
          <!-- 이메일 인증 -->

          <!-- 전화번호 인증 - 미개발로 음영처리 -->
          <el-tab-pane label="전화번호로 찾기" name="phone">
            <div class="form-container-with-overlay">
              <!-- 오버레이 레이어 -->
              <div class="form-overlay">
                <div class="overlay-content">
                  <el-icon
                      :size="40"
                      style="color: var(--el-color-primary);"
                  >
                    <InfoFilled />
                  </el-icon>
                  <p class="overlay-title">서비스 준비 중입니다</p>
                  <p class="overlay-description">현재 전화번호 찾기 기능은 이용할 수 없습니다.</p>
                </div>
              </div>

              <el-form class="find-form" label-position="top">
                <!-- ... 기존 폼 아이템들 ... -->
                <el-form-item label="이름">
                  <el-input
                      disabled
                      placeholder="가입 시 등록한 이름을 입력하세요."
                      size="large"
                  />
                </el-form-item>
                <el-form-item label="전화번호">
                  <el-input
                      disabled
                      placeholder="'-' 없이 숫자만 입력하세요."
                      size="large"
                  >
                    <template #append>
                      <el-button disabled>인증번호 전송</el-button>
                    </template>
                  </el-input>
                </el-form-item>
                <el-form-item label="인증번호" style="margin-bottom: 4px;">
                  <el-input
                      disabled
                      placeholder="수신된 인증번호를 입력하세요."
                      size="large"
                  />
                </el-form-item>
              </el-form>
              <div class="timer-area">
                <el-text class="timer-text">
                  <el-icon class="timer-icon"><Timer /></el-icon>
                </el-text>
                <div style="display: flex; align-items: center;">
                  <el-text style="font-size: 12px;">인증번호가 오지 않나요?</el-text>
                  <el-button :icon="QuestionFilled" type="info" link class="help-icon-button"/>
                </div>
              </div>
            </div>
          </el-tab-pane>
          <!-- 전화번호 인증 - 미개발로 음영처리 -->
        </el-tabs>

        <el-button type="primary" class="action-button" :disabled="!isCodeSent" @click="findId">
          확인
        </el-button>
      </div>

      <!-- 2단계: 아이디 찾기 결과 (복사하기 기능 추가) -->
      <div v-else class="result-section">
        <p class="result-description">고객님의 아이디 찾기 결과입니다.</p>
        <div class="result-box">
          <span>{{ maskedFoundUserId }}</span>
          <el-button
              :icon="DocumentCopy"
              type="info"
              text
              circle
              class="copy-button"
              @click="copyToClipboard(fullFoundUserId)"
          />
        </div>
        <div>
          <el-button @click="onClickToGoPage('FindPassword')">비밀번호 찾기</el-button>
          <el-button type="primary" @click="onClickToGoPage('login')">로그인 하기</el-button>
        </div>
      </div>

      <!-- 인증 전 떠있는 링크 버튼 -->
      <div v-if="!isIdFound" class="find-links">
        <el-button type="info" link @click="onClickToGoPage('login')">로그인 하기</el-button>
        <el-divider direction="vertical" />
        <el-button type="info" link @click="onClickToGoPage('FindPassword')">비밀번호 찾기</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
/* 전체 페이지 레이아웃 */
.find-id-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 100px);
}
/* 아이디 찾기 카드 */
.find-id-card {
  width: 450px;
  padding: 8px;
  box-sizing: border-box;
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
/* 탭 스타일 */
.find-tabs {
  margin-bottom: 20px;
}
.find-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}
.find-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  padding: 0 20px;
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
.help-icon-button {
  font-size: 15px;
}
.help-icon-button:focus {
  outline: 0;
}
.input-with-button :deep(.el-input-group__append) {
  background-color: transparent;
  padding: 0;
}
.input-with-button :deep(.el-input-group__append .el-button) {
  border-radius: 0 var(--el-input-border-radius) var(--el-input-border-radius) 0;
  margin: -1px;
}
/* 결과 표시 섹션 */
.result-section {
  text-align: center;
  padding: 20px 0;
}
.result-description {
  font-size: 15px;
  color: #475669;
  margin-bottom: 20px;
}
.result-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f9fafb;
  border: 1px solid #e5e9f2;
  border-radius: 8px;
  padding: 15px 20px;
  margin-bottom: 30px;
}
.result-box span {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  letter-spacing: 1px;
}
.copy-button {
  font-size: 20px;
  outline: 0;
}
.find-links {
  margin-top: 20px;
  text-align: center;
}
.result-actions .el-button {
  flex-grow: 1;
  height: 48px;
  font-weight: bold;
  font-size: 16px;
}
/* 공통 액션 버튼 */
.action-button {
  width: 100%;
  height: 48px;
  font-weight: bold;
  font-size: 16px;
  margin-top: 10px;
  color: var(--el-bg-color);
}
/* 하단 네비게이션 링크 */
.navigation-links { margin-top: 25px; text-align: center; }
.email-help-container {
  padding: 8px;
  border-radius: 8px;
}
.custom-alert :deep(.el-alert__description) {
  white-space: pre-wrap; /* \n과 같은 공백 문자를 유지하면서 줄바꿈을 허용 */
}
/* el-timeline-item 내부의 폰트 크기나 스타일 조절 */
.email-help-container :deep(.el-timeline-item__content) {
  font-size: 14px;
  line-height: 1.6;
}
b {
  color: #409EFF; /* 강조 텍스트 색상 */
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
.non-outline {
  outline: 0;
  color: var(--el-color-info) !important;
}
.form-container-with-overlay {
  position: relative; /* 오버레이의 기준점이 됨 */
  border-radius: 8px;
  overflow: hidden; /* 컨테이너를 벗어나는 오버레이 부분을 숨김 */
}
.form-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.7); /* 반투명 흰색 배경 */
  backdrop-filter: blur(4px); /* 뒷 배경을 블러 처리 (모던한 느낌) */
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 10; /* 폼보다 위에 위치하도록 설정 */
  text-align: center;
  color: #303133;
}
.overlay-content .overlay-title {
  font-size: 1.2rem;
  font-weight: bold;
  margin: 10px 0 5px 0;
}
.overlay-content .overlay-description {
  font-size: 0.9rem;
  color: var(--el-fill-color);
}
</style>