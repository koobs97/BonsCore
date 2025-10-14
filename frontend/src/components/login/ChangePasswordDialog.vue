<script setup lang="ts">
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
import { Lock, CircleClose, TopRight } from '@element-plus/icons-vue';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { Common } from "@/common/common";

const router = useRouter();

const props = defineProps({
  visible: { type: Boolean, required: true },
  userInfo: { type: Object, required: true }, // 비밀번호 변경 대상 사용자 ID
});
const emit = defineEmits(['update:visible', 'password-changed']);

const formRef = ref<FormInstance>();
const isSubmitting = ref(false);

const passwordForm = reactive({
  currentPassword: '',
  confirmCurrentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
});

const validatePass = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('새 비밀번호를 입력해주세요.'));
  } else {
    if (passwordForm.confirmNewPassword !== '') {
      if (!formRef.value) return;
      formRef.value.validateField('confirmNewPassword');
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

/**
 * validation 체크
 * @param fieldName
 */
const handleFieldValidation = async (fieldName: any) => {
  if(fieldName === 'password') {

    const encryptedPassword = await Common.encryptPassword(passwordForm.currentPassword);
    const param = {
      userId: props.userInfo.userId,
      password: encryptedPassword,
    }
    const response = await Api.post(ApiUrls.VALIDATE_PASSWORD, param);
    if (!response.data) {
      ElMessage({
        message: '현재 비밀번호가 일치하지 않습니다.',
        grouping: true,
        type: 'error',
      })
      passwordForm.confirmCurrentPassword = 'error';
    } else {
      ElMessage.success('비밀번호가 일치합니다.');
      passwordForm.confirmCurrentPassword = 'success';
    }

  }
}

const submitForm = async (formEl: FormInstance | undefined) => {
  if (!formEl) return;
  await formEl.validate(async (valid) => {
    if (valid) {
      isSubmitting.value = true;
      try {

        // 현재 비밀번호 확인 재호출
        await handleFieldValidation('password');

        if(passwordForm.confirmCurrentPassword == 'error') {
          return;
        }

        await Common.customConfirm(
              '비밀번호 변경'
            , '비밀번호를 변경하시겠습니까?'
            , '확인'
            , '취소')

        // 실제 비밀번호 변경 API 호출
        await Api.post(ApiUrls.UPDATE_PASSWORD_AF_LOGIN, {
          userId: props.userInfo.userId,
          password: await Common.encryptPassword(passwordForm.currentPassword),
        });

        ElMessage.success('비밀번호가 성공적으로 변경되었습니다!');
        emit('password-changed'); // 비밀번호 변경 성공 이벤트 발생
        closeDialog();

      } catch (error) {
        if (error === 'cancel') {
          ElMessage.info('비밀번호 변경을 취소했습니다.');
        }
        else {
          ElMessage.error('비밀번호 변경에 실패했습니다.');
        }
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

const goToFindPassword = () => {
  router.push('/FindPassword');
}
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
      top="25vh"
  >
    <el-card shadow="never" class="custom-el-card">
      <div class="dialog-header">
        <el-text class="dialog-title">비밀번호 변경</el-text>
        <el-button :icon="CircleClose" text @click="closeDialog" class="close-btn" />
      </div>
    </el-card>


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
              @blur="() => handleFieldValidation('password')"
          />
        </el-form-item>

        <div class="forgot-password-wrapper">
          <el-button
              link
              size="small"
              class="forgot-password-btn"
              @click="goToFindPassword"
          >
            비밀번호가 기억나지 않으신다면?
            <el-button size="small" style="width: 8px; height: 24px; margin-left: 4px;" :icon="TopRight">

            </el-button>
          </el-button>
        </div>

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
<style>
.change-password-dialog {
  padding: 20px !important;
}
</style>

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
  font-size: 1.2rem;
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
  margin-top: 22px;
  margin-bottom: 4px;
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

.forgot-password-wrapper {
  text-align: right;
  margin-top: 6px;
}

.forgot-password-btn {
  font-size: 12px;
  color: var(--el-text-color-primary); /* 메인 포인트 색 */
  padding: 0;
  transition: color 0.2s ease, text-decoration 0.2s ease;
}

.forgot-password-btn:hover {
  color: var(--el-text-color-primary);
  text-decoration: underline;
}

.el-card {
  border: none; /* 카드 자체의 테두리 제거 */
  padding: 0 !important; /* 카드 내부 패딩 제거 */
}
.custom-el-card {
  background-color: var(--el-fill-color-lighter);
}
.custom-el-card :deep(.el-card__body) {
  padding: 10px 10px 10px 14px !important;
  border-bottom: 1px solid var(--el-border-color-light);
}
</style>