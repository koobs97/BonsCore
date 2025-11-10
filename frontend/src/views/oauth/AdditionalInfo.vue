<template>
  <div class="additional-info-container">
    <div class="background-gradient"></div>
    <el-card class="info-card glass-effect" shadow="never">

      <!-- 프로필 아바타 섹션 -->
      <div class="avatar-section" v-if="userInfo">
        <el-avatar
            :size="80"
            :src="userInfo.profileImageUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
            class="profile-avatar"
        />
        <h3 class="welcome-message">환영합니다, {{ userInfo.userName }}님!</h3>
        <p class="description">서비스를 시작하기 전에 몇 가지만 더 알려주세요.</p>
      </div>
      <div v-else class="loading-placeholder">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="circle" style="width: 80px; height: 80px;" />
            <div style="padding-top: 14px;">
              <el-skeleton-item variant="h3" style="width: 50%;" />
              <el-skeleton-item variant="text" style="width: 80%;" />
            </div>
          </template>
        </el-skeleton>
      </div>

      <!-- 진행률 표시 -->
      <el-progress :percentage="progress" :stroke-width="6" :show-text="false" color="#6a82fb" class="progress-bar" />

      <!-- 정보 입력 폼 -->
      <el-form
          ref="formRef"
          :model="editForm"
          :rules="rules"
          label-position="top"
          class="info-form"
          @validate="handleValidation"
      >
        <el-form-item prop="userName">
          <template #label>
            <div class="form-label-with-icon">
              <el-icon><User /></el-icon>
              <span>이름</span>
            </div>
          </template>
          <el-input v-model.trim="editForm.userName" placeholder="실명을 입력해주세요." size="large" />
        </el-form-item>

        <el-form-item prop="phoneNumber">
          <template #label>
            <div class="form-label-with-icon">
              <el-icon><Phone /></el-icon>
              <span>휴대폰 번호</span>
            </div>
          </template>
          <el-input v-model.trim="editForm.phoneNumber" placeholder="'-' 없이 숫자만 11자리 입력" size="large" />
        </el-form-item>

        <el-form-item prop="birthDate">
          <template #label>
            <div class="form-label-with-icon">
              <el-icon><Calendar /></el-icon>
              <span>생년월일</span>
            </div>
          </template>
          <el-input v-model.trim="editForm.birthDate" placeholder="8자리 숫자로 입력 (예: 19970729)" size="large" />
        </el-form-item>

        <el-form-item prop="genderCode">
          <template #label>
            <div class="form-label-with-icon">
              <el-icon><Male /></el-icon>
              <span>성별</span>
            </div>
          </template>
          <el-radio-group v-model="editForm.genderCode" class="full-width-radio" size="large">
            <el-radio-button label="M">남자</el-radio-button>
            <el-radio-button label="F">여자</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item class="submit-item">
          <el-button
              type="primary"
              class="submit-button"
              @click="submitForm(formRef)"
              :loading="isSubmitting"
          >
            가입 완료하고 시작하기
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { userStore, userState } from '@/store/userStore';
import { User, Phone, Calendar, Male, Female } from '@element-plus/icons-vue';

const router = useRouter();
const store = userStore();
const formRef = ref<FormInstance>();
const isSubmitting = ref(false);

const userInfo = ref<userState | null>(null);

const editForm = reactive({
  userName: '',
  phoneNumber: '',
  birthDate: '',
  genderCode: '',
});

// 각 필드의 유효성 상태를 추적
const validationStatus = reactive({
  userName: false,
  phoneNumber: false,
  birthDate: false,
  genderCode: false,
});

// 진행률 계산
const progress = computed(() => {
  const validFields = Object.values(validationStatus).filter(status => status).length;
  return (validFields / 4) * 100;
});

// 유효성 검사 규칙
const rules = reactive<FormRules>({
  userName: [{ required: true, message: '이름을 입력해주세요.', trigger: 'blur' }],
  phoneNumber: [
    { required: true, message: '휴대폰 번호를 입력해주세요.', trigger: 'blur' },
    { pattern: /^010[0-9]{8}$/, message: '유효한 휴대폰 번호 11자리를 입력해주세요.', trigger: 'blur' }
  ],
  birthDate: [
    { required: true, message: '생년월일을 입력해주세요.', trigger: 'blur' },
    { pattern: /^(19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])$/, message: '유효한 생년월일 8자리를 입력해주세요 (YYYYMMDD).', trigger: 'blur' }
  ],
  genderCode: [{ required: true, message: '성별을 선택해주세요.', trigger: 'change' }],
});

onMounted(() => {
  userInfo.value = store.userInfo;
  if (userInfo.value) {
    editForm.userName = userInfo.value.userName || '';
    // 초기 유효성 검사 (이름은 이미 채워져 있으므로)
    if (editForm.userName) validationStatus.userName = true;
  } else {
    ElMessage.error('비정상적인 접근입니다. 다시 로그인해주세요.');
    router.push('/login');
  }
});

// 유효성 검사 상태 업데이트 핸들러
const handleValidation = (prop: string, isValid: boolean) => {
  if (prop in validationStatus) {
    (validationStatus as any)[prop] = isValid;
  }
};

const submitForm = async (formEl: FormInstance | undefined) => {
  if (!formEl || !userInfo.value) return;
  await formEl.validate(async (valid) => {
    if (valid) {
      isSubmitting.value = true;
      try {
        const payload = {
          userId: userInfo.value.userId,
          ...editForm
        };
        await Api.post(ApiUrls.UPDATE_USER_INFO, payload, true); // 로딩 옵션 추가
        const updatedUserInfo = { ...userInfo.value, ...payload };
        store.setUserInfo(updatedUserInfo);
        ElMessage.success('환영합니다! 회원가입이 성공적으로 완료되었습니다.');
        await router.push('/');
      } catch (error) {
        ElMessage.error('정보 저장 중 오류가 발생했습니다.');
      } finally {
        isSubmitting.value = false;
      }
    } else {
      ElMessage.error('입력되지 않거나 유효하지 않은 항목이 있습니다.');
    }
  });
};
</script>

<style scoped>
/* 전체 페이지 레이아웃: 앱의 기본 배경색 사용 */
.additional-info-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
  box-sizing: border-box;
  background-color: var(--el-bg-color);
}

/* 카드 스타일: 은은한 Glassmorphism 효과 */
.info-card.glass-effect {
  width: 100%;
  max-width: 450px;
  padding: 32px;
  border-radius: 16px;

  /* backdrop-filter로 뒷배경을 흐리게 만듦 */
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);

  /* 배경과 테두리를 앱의 기본 색상과 연동, 투명도 조절 */
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: var(--el-box-shadow-light);
}

/* 상단 아바타 섹션 */
.avatar-section, .loading-placeholder {
  text-align: center;
  margin-bottom: 24px;
}
.loading-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.profile-avatar {
  border: 3px solid var(--el-border-color-extra-light);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.welcome-message {
  font-size: 1.5rem;
  font-weight: 600;
  margin: 16px 0 8px;
  color: var(--el-text-color-primary);
}
.description {
  font-size: 1rem;
  color: var(--el-text-color-secondary);
  margin: 0;
}

/* 프로그레스 바 */
.progress-bar {
  margin: 0 0 32px;
}

/* 폼 스타일 */
.form-label-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: var(--el-text-color-regular);
}
.info-form :deep(.el-form-item) {
  margin-bottom: 20px;
}
.info-form :deep(.el-input__wrapper) {
  transition: box-shadow 0.2s ease, background-color 0.2s ease;
}
.info-form .el-form-item.is-required :deep(.el-form-item__label::before) {
  content: '' !important;
  display: none !important;
}

/* 2. is-required 클래스가 있는 form-item 내부에 우리만의 별표를 생성합니다. */
.info-form .el-form-item.is-required .form-label-with-icon::before {
  content: '*';
  color: var(--el-color-danger);
  margin-right: 4px;
  font-weight: bold;
}
/* 성별 선택 라디오 버튼 */
.full-width-radio {
  width: 100%;
  display: flex;
}
.full-width-radio > .el-radio-button {
  flex: 1;
}
.full-width-radio :deep(.el-radio-button__inner) {
  width: 100%;
}
.full-width-radio :deep(.el-radio-button.is-active .el-radio-button__inner) {
  color: var(--el-bg-color) !important;
}

/* 제출 버튼 */
.submit-item {
  margin-top: 32px;
  margin-bottom: 0;
}
.submit-button {
  width: 100%;
  height: 50px;
  font-weight: bold;
  font-size: 1.1rem;
  border-radius: 8px;
  transition: all 0.3s ease;
}
.submit-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(var(--el-color-primary-rgb), 0.3);
}
</style>