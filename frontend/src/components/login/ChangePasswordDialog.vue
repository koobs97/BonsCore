<script setup lang="ts">
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Lock, CircleClose } from '@element-plus/icons-vue'; // 필요한 아이콘 추가

const props = defineProps({
  visible: { type: Boolean, required: true },
  userInfo: { type: Object, required: true }, // 비밀번호 변경 대상 사용자 ID
});
const emit = defineEmits(['update:visible', 'password-changed']);

const formRef = ref<FormInstance>();
const isSubmitting = ref(false);

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
});

const validatePass = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('새 비밀번호를 입력해주세요.'));
  } else {
    if (passwordForm.confirmNewPassword !== '') {
      if (!formRef.value) return;
      formRef.value.validateField('confirmNewPassword', () => null);
    }
    callback();
  }
};

const validatePass2 = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('새 비밀번호를 다시 입력해주세요.'));
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('새 비밀번호와 일치하지 않습니다.'));
  } else {
    callback();
  }
};

const rules = reactive<FormRules>({
  currentPassword: [
    { required: true, message: '현재 비밀번호를 입력해주세요.', trigger: 'blur' },
    // { min: 6, message: '비밀번호는 최소 6자 이상이어야 합니다.', trigger: 'blur' }, // 실제 요구사항에 따라 수정
  ],
  newPassword: [
    { required: true, validator: validatePass, trigger: 'blur' },
    { min: 6, message: '새 비밀번호는 최소 6자 이상이어야 합니다.', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[!@#$%^&*()])[a-zA-Z\d!@#$%^&*()]{6,}$/, message: '영문, 숫자, 특수문자를 포함해야 합니다.', trigger: 'blur' }, // 강력한 비밀번호 규칙
  ],
  confirmNewPassword: [
    { required: true, validator: validatePass2, trigger: 'blur' },
  ],
});

const submitForm = async (formEl: FormInstance | undefined) => {
  if (!formEl) return;
  await formEl.validate(async (valid) => {
    if (valid) {
      isSubmitting.value = true;
      try {
        // 실제 비밀번호 변경 API 호출
        // const response = await Api.post(ApiUrls.changePassword, {
        //   userId: props.userId,
        //   currentPassword: passwordForm.currentPassword,
        //   newPassword: passwordForm.newPassword,
        // });

        await new Promise(resolve => setTimeout(resolve, 1500)); // API 호출 시뮬레이션

        ElMessage.success('비밀번호가 성공적으로 변경되었습니다!');
        emit('password-changed'); // 비밀번호 변경 성공 이벤트 발생
        closeDialog();
      } catch (error: any) {
        // 에러 처리: API 응답에 따라 구체적인 메시지 표시
        const errorMessage = error.response?.data?.message || '비밀번호 변경에 실패했습니다.';
        ElMessage.error(errorMessage);
        console.error('Password change error:', error);
      } finally {
        isSubmitting.value = false;
      }
    }
  });
};

const closeDialog = () => {
  // 폼 필드 초기화 및 유효성 검사 메시지 초기화
  formRef.value?.resetFields();
  emit('update:visible', false);
};
</script>

<template>
  <el-dialog
      :model-value="visible"
      :show-close="false"
      width="450px"
      :before-close="closeDialog"
      :close-on-click-modal="false"
      append-to-body
      class="change-password-dialog"
      top="30vh"
  >
    <div class="dialog-header">
      <h3 class="dialog-title">비밀번호 변경</h3>
      <el-button :icon="CircleClose" text @click="closeDialog" class="close-btn" />
    </div>

    <div class="dialog-body">
      <el-form
          ref="formRef"
          :model="passwordForm"
          :rules="rules"
          label-position="top"
          class="password-form"
      >
        <el-form-item label="현재 비밀번호" prop="currentPassword">
          <el-input
              v-model="passwordForm.currentPassword"
              type="password"
              show-password
              :prefix-icon="Lock"
              placeholder="현재 비밀번호를 입력하세요"
          />
        </el-form-item>

        <el-form-item label="새 비밀번호" prop="newPassword">
          <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              :prefix-icon="Lock"
              placeholder="새 비밀번호를 입력하세요 (6자 이상)"
          />
        </el-form-item>

        <el-form-item label="새 비밀번호 확인" prop="confirmNewPassword">
          <el-input
              v-model="passwordForm.confirmNewPassword"
              type="password"
              show-password
              :prefix-icon="Lock"
              placeholder="새 비밀번호를 다시 입력하세요"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeDialog">취소</el-button>
        <el-button
            type="primary"
            @click="submitForm(formRef)"
            :loading="isSubmitting"
        >
          비밀번호 변경
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
/* Dialog Header Customization */
.change-password-dialog :deep(.el-dialog__header) {
  padding: 20px 24px 10px 24px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-right: 0; /* 기본 마진 제거 */
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:global(body .change-password-dialog .el-dialog__header) {
  display: none;
  padding: 0;
  margin: 0;
}

.change-password-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}

.change-password-dialog :deep(.el-dialog__footer) {
  padding: 10px 24px 20px 24px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dialog-title {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.close-btn {
  font-size: 20px;
  color: var(--el-text-color-secondary);
  transition: all 0.2s ease;
  padding: 8px 4px;
}
.close-btn:hover {
  color: var(--el-color-primary);
  transform: rotate(90deg);
}

.password-form {
  padding-top: 10px;
}

.el-form-item {
  margin-bottom: 22px;
}

.dialog-footer {
  text-align: right;
}

/* 폼 아이템 에러 메시지 스타일은 UserEditForm과 동일하게 유지 */
.el-form-item__error {
  color: var(--el-color-danger);
  font-size: 0.75rem;
  line-height: 1.2;
  margin-top: 4px;
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  text-align: left;
  opacity: 0;
  transform: translateY(-5px);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.el-form-item.is-error .el-form-item__error {
  opacity: 1;
  transform: translateY(0);
}

.el-form-item.is-error :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--el-color-danger) inset !important;
}

.el-form-item.is-error :deep(.el-form-item__error) {
  color: var(--el-color-danger);
  font-size: 11px;
  left: 0;
  line-height: 1;
  padding-top: 2px;
  font-weight: 700;
  position: absolute;
  top: 100%;
}
</style>