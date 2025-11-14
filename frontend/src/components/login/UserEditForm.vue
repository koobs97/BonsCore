<script setup lang="ts">
import { ref, reactive, watch, defineProps, defineEmits, computed } from 'vue';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus'
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { User, Phone, Key, Message, Calendar, RefreshRight, QuestionFilled } from '@element-plus/icons-vue'
import { Dialogs } from "@/common/dialogs";
import ChangePasswordDialog from "@/components/login/ChangePasswordDialog.vue";
import SecurityQuestionWizardDialog from "@/components/login/SecurityQuestionWizardDialog.vue";
import { useI18n } from "vue-i18n";
import {userState, userStore} from "@/store/userStore";
import {Common} from "@/common/common";

const { t, locale } = useI18n();
const userStoreObj = userStore();

const changePasswordDialogVisible = ref(false);
const securityQuestionDialogVisible = ref(false);

// --- Props & Emits ---
const props = defineProps({
  visible: { type: Boolean, required: true },
  userData: { type: Object, required: true }
});
const emit = defineEmits(['update:visible', 'update-success']);

// --- 상태(State) 관리 ---
const formRef = ref<FormInstance>();
const isSubmitting = ref(false);

// 현재 언어에 따라 사용자 이름을 반환
const displayedUserName = computed(() => {
  const User = userStoreObj.getUserInfo
  return locale.value === 'en' ? User.userNameEn : User.userName;
});

const editForm = reactive({
  userName: '',
  phoneNumber: '',
  email: '',
  birthDate: '',
  genderCode: '',
  userEmailCheckStatus: '',
});

// --- 폼 유효성 검사 규칙 (기존과 동일) ---
const rules = reactive<FormRules>({
  userName: [
    { required: true, message: t('userEditForm.rules.userName.required'), trigger: 'blur' },
    { min: 2, max: 10, message: t('userEditForm.rules.userName.length'), trigger: 'blur' },
  ],
  phoneNumber: [
    { required: true, message: t('userEditForm.rules.phoneNumber.required'), trigger: 'blur' },
    { pattern: /^\d{2,3}-?\d{3,4}-?\d{4}$/, message: t('userEditForm.rules.phoneNumber.pattern'), trigger: 'blur' }
  ],
  email: [
    { required: true, message: t('userEditForm.rules.email.required'), trigger: 'blur' },
    { min: 5, max: 50, message: t('userEditForm.rules.email.length'), trigger: 'blur' },
  ],
  birthDate: [
    { required: true, message: t('userEditForm.rules.birthDate.required'), trigger: 'blur' },
    { min: 5, max: 50, message: t('userEditForm.rules.birthDate.length'), trigger: 'blur' },
  ],
});

// --- 로직(Logic) ---
watch(() => props.visible, (newUserData) => {
  if (props.visible) {
    editForm.userName = props.userData.userName || '';
    editForm.phoneNumber = props.userData.phoneNumber || '';
    editForm.email = props.userData.email || '';
    editForm.birthDate = props.userData.birthDate || '';
    editForm.genderCode = props.userData.genderCode || '';

    formRef.value?.clearValidate();
  }
}, { immediate: true });

/**
 * 원본 정보 다시 불러오기
 */
const onClickRollBackInfo = () => {
  editForm.userName = props.userData.userName || '';
  editForm.phoneNumber = props.userData.phoneNumber || '';
  editForm.email = props.userData.email || '';
  editForm.birthDate = props.userData.birthDate || '';
  editForm.genderCode = props.userData.genderCode || '';

  formRef.value?.clearValidate();
}

const submitForm = async (formEl: FormInstance | undefined) => {
  if (!formEl) return;
  await formEl.validate(async (valid) => {
    if (valid) {
      isSubmitting.value = true;


      // 이메일 유효성 재검사
      // 현재 사용중인 이메일과 같으면 return;
      if(editForm.email === props.userData.email) {
        editForm.userEmailCheckStatus = 'success';
      }
      else {
        const response2 = await Api.post(ApiUrls.CHECK_EMAIL, { email: editForm.email });
        if (response2.data) {
          ElMessage({ message: t('userEditForm.messages.emailUnavailable'), grouping: true, type: 'error' })
          editForm.userEmailCheckStatus = 'error';
          return;
        }
        else {
          editForm.userEmailCheckStatus = 'success';
        }
      }

      // 이메일이 유효할 때
      if(editForm.userEmailCheckStatus === 'success') {
        try {

          await Dialogs.customConfirm(
              t('userEditForm.dialogs.confirmUpdateTitle'),
              t('userEditForm.dialogs.confirmUpdateMessage'),
              t('userEditForm.dialogs.confirmButton'),
              t('userEditForm.dialogs.cancelButton'),
              '485px',
          );

          const payload = {
            userId: props.userData.userId,
            email : editForm.email,
            userName: editForm.userName,
            phoneNumber: editForm.phoneNumber.replace(/-/g, ''),
            birthDate : editForm.birthDate.replace(/\D/g, ""),
            genderCode : editForm.genderCode,
          };

          await Api.post(ApiUrls.UPDATE_USER_INFO, payload);

          ElMessage.success(t('userEditForm.messages.updateSuccess'));
          await Common.setUser();

          emit('update-success');

          window.location.reload();
        } catch (error) {
          if (error === 'cancel') {
            ElMessage.info(t('userEditForm.messages.updateCancelled'));
          } else {
            ElMessage.error(t('userEditForm.messages.updateFailed'));
          }
        } finally {
          isSubmitting.value = false;
        }
      }

    }
  });
};

const handleChangePassword = () => {
  changePasswordDialogVisible.value = true; // 비밀번호 변경 다이얼로그 열기
}

const closeDialog = () => {
  emit('update:visible', false);
};

const handlePasswordChanged = () => {
  // ElMessage.success('비밀번호가 성공적으로 변경되었습니다.');
  changePasswordDialogVisible.value = false;
};

const handleSetSecurityQuestion = () => {
  securityQuestionDialogVisible.value = true;
}

const handleSecurityQuestionSet = async () => {
  securityQuestionDialogVisible.value = false; // 다이얼로그 닫기
  ElMessage.success(t('userEditForm.messages.securityQuestionSet'));
}

/**
 * validation 체크
 * @param fieldName
 */
const handleFieldValidation = async (fieldName: any) => {

  // 현재 사용중인 이메일과 같으면 return;
  if(editForm.email === props.userData.email) {
    editForm.userEmailCheckStatus = 'success';
    return;
  }

  if(fieldName === 'email') {
    const param = {
      email: editForm.email
    }
    const response = await Api.post(ApiUrls.CHECK_EMAIL, param);
    if (response.data) {
      ElMessage({ message: t('userEditForm.messages.emailUnavailable'), grouping: true, type: 'error' });
      editForm.userEmailCheckStatus = 'error';
    } else {
      ElMessage.success(t('userEditForm.messages.emailAvailable'));
      editForm.userEmailCheckStatus = 'success';
    }

  }
}
</script>

<template>
  <el-dialog
      :model-value="visible"
      :show-close="false"
      :width="locale === 'en' ? '770px' : '700px'"
      :before-close="closeDialog"
      :close-on-click-modal="false"
      append-to-body
      class="premium-dialog"
      draggable
  >
    <div class="dialog-layout">
      <!-- 좌측 사이드바: 프로필 정보 -->
      <div class="sidebar">
        <el-avatar
            :size="80"
            src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png"
            style="border: 3px solid #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.1);"
        />
        <h3 class="user-name">{{ displayedUserName }}</h3>
        <p class="user-email">{{ props.userData.email }}</p>
        <el-tag effect="light" type="info" size="small" round>
          User ID: {{ props.userData.userId }}
        </el-tag>
      </div>

      <!-- 우측 폼 컨텐츠 -->
      <div class="form-content">

        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px;">
          <h3 class="form-title">{{ t('userEditForm.form.title') }}</h3>
          <div style="margin: 0;">
            <el-button
                type="info"
                :icon="RefreshRight"
                round
                class="refresh-modern-btn"
                @click="onClickRollBackInfo"
            >
              {{ t('userEditForm.form.buttonRestore') }}
            </el-button>
          </div>

        </div>

        <el-form
            ref="formRef"
            :model="editForm"
            :rules="rules"
            label-position="left"
            label-width="100px"
        >
          <!-- Public Information Section -->
          <h4 class="section-title" />

          <el-form-item
              :label="t('userEditForm.form.labels.name')"
              prop="userName">
            <el-input
                v-model="editForm.userName"
                :prefix-icon="User"
                :placeholder="t('userEditForm.form.placeholders.name')" />
          </el-form-item>

          <el-form-item
              :label="t('userEditForm.form.labels.email')"
              prop="email">
            <el-input
                v-model="editForm.email"
                :prefix-icon="Message"
                placeholder="example@email.com"
                @blur="() => handleFieldValidation('email')"
            />
          </el-form-item>

          <el-form-item
              :label="t('userEditForm.form.labels.phone')"
              prop="phoneNumber">
            <el-input
                v-model="editForm.phoneNumber"
                :prefix-icon="Phone"
                :placeholder="t('userEditForm.form.placeholders.phone')" />
          </el-form-item>

          <el-form-item
              :label="t('userEditForm.form.labels.birthDate')"
              prop="birthDate">
            <el-input
                v-model="editForm.birthDate"
                :prefix-icon="Calendar"
                :placeholder="t('userEditForm.form.placeholders.birthDate')" />
          </el-form-item>

          <el-form-item
              :label="t('userEditForm.form.labels.gender')"
              prop="phoneNumber">
            <div class="my-radio-group">
                <el-radio-group v-model="editForm.genderCode">
                  <el-radio-button label="M">{{ t('userEditForm.form.gender.male') }}</el-radio-button>
                  <el-radio-button label="F">{{ t('userEditForm.form.gender.female') }}</el-radio-button>
                </el-radio-group>
            </div>
          </el-form-item>

          <!-- Security Section -->
          <h4 class="section-title-footer">{{ t('userEditForm.form.sectionSecurity') }}</h4>
          <div class="security-card">
            <div class="security-menu-item">
              <div class="item-info">
                <el-icon><Key /></el-icon>
                <div class="item-text">
                  <span class="item-title">{{ t('userEditForm.security.password') }}</span>
                  <span class="item-desc">{{ t('userEditForm.security.passwordDesc') }}</span>
                </div>
              </div>
              <el-button text bg @click="handleChangePassword">{{ t('userEditForm.security.buttonChange') }}</el-button>
            </div>

            <el-divider />

            <div class="security-menu-item">
              <div class="item-info">
                <el-icon><QuestionFilled /></el-icon>
                <div class="item-text">
                  <span class="item-title">{{ t('userEditForm.security.securityQuestion') }}</span>
                  <span class="item-desc">{{ t('userEditForm.security.securityQuestionDesc') }}</span>
                </div>
              </div>
              <el-button
                  text
                  bg
                  @click="handleSetSecurityQuestion"
              >
                {{ t('userEditForm.security.buttonSet') }}
              </el-button>
            </div>
          </div>
          <ChangePasswordDialog
              v-model:visible="changePasswordDialogVisible"
              :user-info="props.userData"
              @password-changed="handlePasswordChanged"
          />
          <SecurityQuestionWizardDialog
              v-model:visible="securityQuestionDialogVisible"
              @success="handleSecurityQuestionSet"
          />
        </el-form>

        <!-- Footer -->
        <div class="dialog-footer">
          <el-button @click="closeDialog">{{ t('userEditForm.footer.buttonCancel') }}</el-button>
          <el-button
              type="primary"
              @click="submitForm(formRef)"
              :loading="isSubmitting"
          >
            {{ t('userEditForm.footer.buttonUpdate') }}
          </el-button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>


<style scoped>
/* body에서 시작하는 선택자로 우선순위를 높입니다. */
:global(body .premium-dialog .el-dialog__header) {
  display: none;
  padding: 0;
  margin: 0;
}

/* :global()을 사용하여 scoped 스타일 외부의 body를 참조합니다. */
:global(body .premium-dialog .el-dialog__body) {
  padding: 0;
}
/* Dialog 레이아웃 초기화 */
.premium-dialog :deep(.el-dialog__header) {
  display: none !important;
  padding: 0 !important;
  margin: 0 !important;
}
.premium-dialog :deep(.el-dialog__body) {
  padding: 0 !important;
}
.premium-dialog .el-dialog {
  --el-dialog-border-radius: 12px;
}

.dialog-layout {
  display: flex;
  background-color: var(--el-bg-color);
}

/* 좌측 사이드바 */
.sidebar {
  width: 200px;
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #f7f8fc 0%, #eef2f7 100%);
  border-right: 1px solid var(--el-border-color-light);
}
.dark .sidebar {
  background: linear-gradient(160deg, #2c313a 0%, #22262d 100%);
}

.user-name {
  margin: 16px 0 4px;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.user-email {
  margin: 0 0 6px;
  font-size: 0.8rem;
  color: var(--el-text-color-secondary);
}

/* 우측 폼 컨텐츠 */
.form-content {
  flex: 1;
  padding: 20px 12px 12px 32px;
  position: relative;
  display: flex;
  flex-direction: column;
}

.form-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

/* 폼 사이 위아래 공간 */
.el-form-item {
  margin-bottom: 20px;
}

.section-title {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--el-text-color-secondary);
  margin: 24px 0 8px;
  font-weight: 600;
}

.section-title-footer {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--el-text-color-secondary);
  margin: 80px 0 8px;
  font-weight: 600;
}

.security-card {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-light);
  background-color: var(--el-fill-color-lighter);
}
.security-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}
.item-text {
  display: flex;
  flex-direction: column;
  margin-left: 12px;
}
.item-title {
  font-weight: 500;
  color: var(--el-text-color-primary);
}
.item-desc {
  font-size: 0.8rem;
  color: var(--el-text-color-secondary);
}
/* Divider의 기본 마진이 너무 커서 조정합니다. */
.security-card .el-divider {
  margin: 0;
}
.item-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

/* 폼과 푸터 분리 */
.el-form {
  flex-grow: 1;
}

/* 푸터 */
.dialog-footer {
  text-align: right;
  padding-top: 24px;
  margin-top: auto;
}
.my-radio-group :deep(.el-radio-button),
.my-radio-group :deep(.el-radio-button.is-active .el-radio-button__inner) {
  /* 여기에 선택된 버튼 자체의 스타일을 적용합니다. */
  color: var(--el-bg-color) !important;
}
.el-form-item__error {
  color: var(--el-color-danger); /* Element-UI의 경고 색상 변수 사용 */
  font-size: 0.75rem; /* 12px 대신 rem 사용 */
  line-height: 1.2; /* 가독성을 위해 line-height 추가 */
  margin-top: 4px; /* 입력 필드와의 간격 조정 */
  position: absolute; /* 기존과 동일 */
  top: 100%; /* 기존과 동일 */
  left: 0; /* 기존과 동일 */
  width: 100%; /* 에러 메시지가 길어질 경우를 대비해 너비 지정 */
  text-align: left; /* 텍스트 정렬 */
  opacity: 0; /* 초기에는 숨김 */
  transform: translateY(-5px); /* 약간 위로 올려서 자연스러운 애니메이션 준비 */
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1); /* 부드러운 애니메이션 */
}

/* 에러가 활성화되었을 때 */
.el-form-item.is-error .el-form-item__error {
  opacity: 1; /* 보이게 함 */
  transform: translateY(0); /* 제자리로 이동 */
}

/* 추가적으로, 입력 필드 자체에 에러 스타일을 적용하여 시각적 강조 */
.el-form-item.is-error :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--el-color-danger) inset !important; /* 에러 시 붉은 테두리 */
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

.refresh-modern-btn {
  background-color: var(--refresh-modern-color); /* 밝은 회색 톤 */
  color: var(--refresh-modern-text-color); /* 다크 그레이 텍스트 */
  border: 1px solid #e5e7eb; /* 은은한 테두리 */
  font-size: 12px;
  font-weight: 500;
  height: 30px;
  padding: 0 12px; /* 좌우 여백 넉넉하게 → pill 버튼 느낌 */
  transition: all 0.25s ease;
}

.refresh-modern-btn:hover {
  background-color: var(--refresh-modern-border-color);
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
  transform: translateY(-1px); /* 살짝 떠오르는 효과 */
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.refresh-modern-btn:active {
  transform: scale(0.96); /* 클릭 시 눌리는 효과 */
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
</style>