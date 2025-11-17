<script setup lang="ts">
import { reactive, ref, computed } from 'vue';
import { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElNotification } from 'element-plus';
import { ArrowRight, CircleCheck } from '@element-plus/icons-vue';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { Common } from "@/common/common";
import { Dialogs } from "@/common/dialogs";
import { useI18n } from 'vue-i18n';

const { t, locale } = useI18n();

// --- Props & Emits ---
const props = defineProps({
  visible: { type: Boolean, required: true },
});
const emit = defineEmits(['update:visible', 'success']);

// --- 상태(State) 관리 ---
const formRef = ref<FormInstance>();
const isSubmitting = ref(false);
const activeStep = ref(0); // 현재 활성화된 단계 (0: 본인 확인, 1: 질문 설정)

const form = reactive({
  question: '',
  answer: '',
  currentPassword: '',
});

// 보안 질문 목록
const securityQuestions = ref<{ value: string; label: string; }[]>([]);

const dialogWidth = computed(() => {
  return locale.value === 'en' ? '700px' : '600px';
});

/**
 * 보안질문 목록 가져오기
 */
const fetchSecurityQuestions = async () => {
  const response = await Api.post(ApiUrls.GET_PASSWORD_HINT, {});
  if (response.data && Array.isArray(response.data)) {
    securityQuestions.value = response.data.map((item: any) => {
      return {
        value: item.questionCode,
        label: t(`securityQuestions.${item.questionCode}`),
      };
    });
  }
};

// --- 유효성 검사 규칙 ---
const rules = reactive<FormRules>({
  question: [{ required: true, message: t('securityQuestionSetup.validation.selectQuestion'), trigger: 'change' }],
  answer: [
    { required: true, message: t('securityQuestionSetup.validation.enterAnswer'), trigger: 'blur' },
    { min: 2, max: 20, message: t('securityQuestionSetup.validation.answerLength'), trigger: 'blur' },
  ],
  currentPassword: [
    { required: true, message: t('securityQuestionSetup.validation.enterCurrentPassword'), trigger: 'blur' }
  ],
});

// 폼 상태를 초기화하는 함수
const resetForm = () => {
  activeStep.value = 0;
  formRef.value?.resetFields();
};

// 다이얼로그를 닫는 함수
const closeDialog = () => {
  emit('update:visible', false);
  // 다이얼로그 닫힘 애니메이션 시간을 고려하여 폼 상태를 초기화합니다.
  setTimeout(resetForm, 300);
};

// 다음 단계로 이동하는 함수 (비밀번호 검증)
const handleNext = async () => {
  const formEl = formRef.value;
  if (!formEl) return;
  // 현재 단계의 필드만 유효성 검사
  const valid = await formEl.validateField('currentPassword').catch(() => false);

  if (valid) {
    isSubmitting.value = true;
    const encryptedPassword = await Common.encryptPassword(form.currentPassword);
    const response = await Api.post(ApiUrls.VALIDATE_PASSWORD, { password: encryptedPassword });
    if(response.data) {
      ElMessage.success(t('securityQuestionSetup.messages.identityVerified'));
      activeStep.value++;

      await fetchSecurityQuestions();
    }
    else {
      ElMessage.error(t('securityQuestionSetup.messages.passwordMismatch'));
    }
    isSubmitting.value = false;
  }
};

// 최종 제출 함수 (보안 질문 저장)
const handleSubmit = async () => {
  const formEl = formRef.value;
  if (!formEl) return;

  await Promise.all([
    formEl.validateField('question'),
    formEl.validateField('answer')
  ]);

  // 질문/답변 설정 다이얼로그
  await Dialogs.customConfirm(
      t('securityQuestionSetup.messages.confirmTitle'),
      t('securityQuestionSetup.messages.confirmMessage'),
      t('securityQuestionSetup.buttons.save'),
      t('securityQuestionSetup.buttons.cancel'),
      '460px',
  );

  isSubmitting.value = true;
  const payload = { passwordHint: form.question, passwordHintAnswer: form.answer };
  await Api.post(ApiUrls.UPDATE_PASSWORD_HINT, payload);

  ElNotification({
    title: t('securityQuestionSetup.messages.notificationTitle'),
    message: t('securityQuestionSetup.messages.notificationMessage'),
    type: 'success',
  });

  emit('success'); // 부모 컴포넌트에 성공 이벤트 전달
  closeDialog(); // 성공 후 다이얼로그 닫기
  isSubmitting.value = false;
};
</script>

<template>
  <el-dialog
      :model-value="visible"
      :title="t('securityQuestionSetup.title')"
      :width="dialogWidth"
      :before-close="closeDialog"
      :close-on-click-modal="false"
      class="security-dialog"
  >
    <!-- 단계 표시기 -->
    <el-steps :active="activeStep" finish-status="success" simple style="margin-bottom: 30px;">
      <el-step :title="t('securityQuestionSetup.steps.step1')" />
      <el-step :title="t('securityQuestionSetup.steps.step2')" />
    </el-steps>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <!-- Step 1: 비밀번호 확인 -->
      <div v-show="activeStep === 0">
        <el-card shadow="never" class="step-card">
          <p class="step-guide">
            {{ t('securityQuestionSetup.step1.guide') }}
          </p>
          <el-form-item :label="t('securityQuestionSetup.step1.label')" prop="currentPassword">
            <el-input
                v-model="form.currentPassword"
                type="password"
                :placeholder="t('securityQuestionSetup.step1.placeholder')"
                show-password
                size="large"
                @keyup.enter="handleNext"
            />
          </el-form-item>
        </el-card>
      </div>

      <!-- Step 2: 질문/답변 설정 -->
      <div v-show="activeStep === 1">
        <el-card shadow="never" class="step-card">
          <p class="step-guide" style="white-space: pre-line;">
            {{ t('securityQuestionSetup.step2.guide') }}
          </p>
          <el-form-item :label="t('securityQuestionSetup.step2.questionLabel')" prop="question">
            <el-select
                v-model="form.question"
                :placeholder="t('securityQuestionSetup.step2.questionPlaceholder')"
                style="width: 100%;"
                size="large"
            >
              <el-option
                  v-for="item in securityQuestions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('securityQuestionSetup.step2.answerLabel')" prop="answer">
            <el-input
                v-model="form.answer"
                :placeholder="t('securityQuestionSetup.step2.answerPlaceholder')"
                size="large"
            />
          </el-form-item>
        </el-card>
      </div>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeDialog">{{ t('securityQuestionSetup.buttons.cancel') }}</el-button>
        <!-- 첫 번째 단계에서 '다음' 버튼 표시 -->
        <el-button
            v-if="activeStep === 0"
            type="primary"
            @click="handleNext"
            :loading="isSubmitting"
            :icon="ArrowRight"
        >
          {{ t('securityQuestionSetup.buttons.next') }}
        </el-button>
        <!-- 두 번째 단계에서 '완료' 버튼 표시 -->
        <el-button
            v-if="activeStep === 1"
            type="primary"
            @click="handleSubmit"
            :loading="isSubmitting"
            :icon="CircleCheck"
        >
          {{ t('securityQuestionSetup.buttons.complete') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.security-dialog {
  .step-card {
    background-color: var(--el-fill-color-lighter);
    border: 1px solid var(--el-border-color-lighter);
    padding: 20px;
  }

  .step-guide {
    font-size: 14px;
    color: var(--el-text-color-secondary);
    line-height: 1.6;
    margin-top: 0;
    margin-bottom: 24px;
  }

  .el-form-item {
    margin-bottom: 22px;
  }
}
</style>