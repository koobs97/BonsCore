<script setup lang="ts">
import { reactive, ref} from 'vue';
import { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElNotification } from 'element-plus';
import { ArrowRight, CircleCheck } from '@element-plus/icons-vue';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { Common } from "@/common/common";
import { Dialogs } from "@/common/dialogs";

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

/**
 * 보안질문 목록 가져오기
 */
const fetchSecurityQuestions = async () => {
  const response = await Api.post(ApiUrls.GET_PASSWORD_HINT, {});
  if (response.data && Array.isArray(response.data)) {
    securityQuestions.value = response.data.map((item: any) => {
      return {
        value: item.questionCode,
        label: item.questionText,
      };
    });
  }
};

// --- 유효성 검사 규칙 ---
const rules = reactive<FormRules>({
  question: [{ required: true, message: '보안 질문을 선택해주세요.', trigger: 'change' }],
  answer: [
    { required: true, message: '답변을 입력해주세요.', trigger: 'blur' },
    { min: 2, max: 20, message: '답변은 2자 이상 20자 이하로 입력해야 합니다.', trigger: 'blur' },
  ],
  currentPassword: [
    { required: true, message: '현재 비밀번호를 입력해주세요.', trigger: 'blur' }
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
      ElMessage.success('본인 확인이 완료되었습니다.');
      activeStep.value++;

      await fetchSecurityQuestions();
    }
    else {
      ElMessage.error('비밀번호가 일치하지 않습니다.');
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

  await Dialogs.customConfirm(
      '질문/답변 설정',
      '입력하신 질문과 답변을 저장하시겠습니까?',
      '저장하기',
      '취소',
      '460px',
  );

  isSubmitting.value = true;
  const payload = { passwordHint: form.question, passwordHintAnswer: form.answer };
  await Api.post(ApiUrls.UPDATE_PASSWORD_HINT, payload);

  ElNotification({
    title: '설정 완료',
    message: '보안 질문이 성공적으로 설정되었습니다.',
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
      title="보안 질문 설정"
      width="600px"
      :before-close="closeDialog"
      :close-on-click-modal="false"
      class="security-dialog"
  >
    <!-- 단계 표시기 -->
    <el-steps :active="activeStep" finish-status="success" simple style="margin-bottom: 30px;">
      <el-step title="본인 확인" />
      <el-step title="질문/답변 설정" />
    </el-steps>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <!-- Step 1: 비밀번호 확인 -->
      <div v-show="activeStep === 0">
        <el-card shadow="never" class="step-card">
          <p class="step-guide">
            계정 보안을 위해 현재 비밀번호를 입력하여 본인임을 확인해주세요.
          </p>
          <el-form-item label="현재 비밀번호 확인" prop="currentPassword">
            <el-input
                v-model="form.currentPassword"
                type="password"
                placeholder="현재 사용 중인 비밀번호"
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
          <p class="step-guide">
            비밀번호 분실 시 계정을 찾기 위한 질문과 답변을 설정합니다.<br/>
            본인만 알 수 있는 답변을 신중하게 입력해주세요.
          </p>
          <el-form-item label="질문 선택" prop="question">
            <el-select
                v-model="form.question"
                placeholder="보안 질문을 선택하세요"
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
          <el-form-item label="답변 입력" prop="answer">
            <el-input
                v-model="form.answer"
                placeholder="선택한 질문에 대한 답변을 입력하세요"
                size="large"
            />
          </el-form-item>
        </el-card>
      </div>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeDialog">취소</el-button>
        <!-- 첫 번째 단계에서 '다음' 버튼 표시 -->
        <el-button
            v-if="activeStep === 0"
            type="primary"
            @click="handleNext"
            :loading="isSubmitting"
            :icon="ArrowRight"
        >
          다음
        </el-button>
        <!-- 두 번째 단계에서 '완료' 버튼 표시 -->
        <el-button
            v-if="activeStep === 1"
            type="primary"
            @click="handleSubmit"
            :loading="isSubmitting"
            :icon="CircleCheck"
        >
          완료
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