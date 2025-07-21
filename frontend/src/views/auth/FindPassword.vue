<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useRouter } from "vue-router";
import { ElMessage } from 'element-plus';
import { User, Lock, Key, CircleCheckFilled } from '@element-plus/icons-vue';
import TheFooter from "@/components/layout/TheFooter.vue";

// Vue 라우터 인스턴스
const router = useRouter();

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
.find-form {
  margin-top: 15px;
}
.find-form .el-form-item {
  margin-bottom: 20px;
}
.find-form :deep(.el-form-item__label) {
  font-size: 14px;
  color: #475669;
  padding-bottom: 6px;
  line-height: normal;
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
  margin-top: 10px;
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
</style>