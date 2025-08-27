<script setup lang="ts">
import { ref, reactive, watch, defineProps, defineEmits } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus'
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
// 아이콘 import 추가
import { User, Phone, Key, Close } from '@element-plus/icons-vue'

// --- Props & Emits (기존과 동일) ---
const props = defineProps({
  visible: { type: Boolean, required: true },
  userData: { type: Object, required: true }
});
const emit = defineEmits(['update:visible', 'update-success']);

// --- 상태(State) 관리 ---
const formRef = ref<FormInstance>();
const isSubmitting = ref(false);

const editForm = reactive({
  userName: '',
  phoneNumber: '',
});

// --- 폼 유효성 검사 규칙 (기존과 동일) ---
const rules = reactive<FormRules>({
  userName: [
    { required: true, message: '사용자 이름을 입력해주세요.', trigger: 'blur' },
    { min: 2, max: 10, message: '2자에서 10자 사이로 입력해주세요.', trigger: 'blur' },
  ],
  phoneNumber: [
    { required: true, message: '전화번호를 입력해주세요.', trigger: 'blur' },
    { pattern: /^\d{2,3}-?\d{3,4}-?\d{4}$/, message: '유효한 전화번호 형식이 아닙니다.', trigger: 'blur' }
  ],
});

// --- 로직(Logic) ---
watch(() => props.userData, (newUserData) => {
  if (newUserData) {
    editForm.userName = newUserData.userName || '';
    editForm.phoneNumber = newUserData.phoneNumber || '';
  }
}, { immediate: true });

const submitForm = async (formEl: FormInstance | undefined) => {
  if (!formEl) return;
  await formEl.validate(async (valid) => {
    if (valid) {
      isSubmitting.value = true;
      try {
        const payload = {
          userId: props.userData.userId,
          userName: editForm.userName,
          phoneNumber: editForm.phoneNumber.replace(/-/g, ''),
        };
        await new Promise(resolve => setTimeout(resolve, 1000)); // API 호출 시뮬레이션

        ElMessage.success('프로필 정보가 성공적으로 업데이트되었습니다.');
        emit('update-success');
        closeDialog();
      } catch (error) {
        ElMessage.error('정보 업데이트에 실패했습니다.');
      } finally {
        isSubmitting.value = false;
      }
    }
  });
};

const handleChangePassword = () => {
  ElMessageBox.alert('이곳에서 비밀번호 변경 로직을 담은 새로운 팝업을 띄울 수 있습니다.', '기능 안내', {
    confirmButtonText: '확인',
  })
}

const closeDialog = () => {
  emit('update:visible', false);
};
</script>

<template>
  <el-dialog
      :model-value="visible"
      :show-close="false"
      width="670px"
      :before-close="closeDialog"
      :close-on-click-modal="false"
      append-to-body
      class="premium-dialog"
  >
    <div class="dialog-layout">
      <!-- 좌측 사이드바: 프로필 정보 -->
      <div class="sidebar">
        <el-avatar
            :size="80"
            src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png"
            style="border: 3px solid #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.1);"
        />
        <h3 class="user-name">{{ props.userData.userName }}</h3>
        <p class="user-email">{{ props.userData.email }}</p>
        <el-tag effect="light" type="info" size="small" round>
          User ID: {{ props.userData.userId }}
        </el-tag>
      </div>

      <!-- 우측 폼 컨텐츠 -->
      <div class="form-content">
        <h3 class="form-title">계정 설정</h3>

        <el-form
            ref="formRef"
            :model="editForm"
            :rules="rules"
            label-position="top"
        >
          <!-- Public Information Section -->
          <h4 class="section-title"></h4>
          <el-form-item label="사용자 이름" prop="userName">
            <el-input v-model="editForm.userName" size="large" :prefix-icon="User" placeholder="사용하실 이름을 입력하세요" />
          </el-form-item>

          <!-- Contact Information Section -->
          <el-form-item label="연락처" prop="phoneNumber">
            <el-input v-model="editForm.phoneNumber" size="large" :prefix-icon="Phone" placeholder="'-'를 제외하고 숫자만 입력하세요" />
          </el-form-item>

          <!-- Security Section -->
          <h4 class="section-title-footer">보안 설정</h4>
          <div class="security-item">
            <div class="item-info">
              <el-icon><Key /></el-icon>
              <span>비밀번호</span>
            </div>
            <el-button text bg @click="handleChangePassword">변경</el-button>
          </div>
        </el-form>

        <!-- Footer -->
        <div class="dialog-footer">
          <el-button @click="closeDialog" size="large" style="margin-right: 4px;">취소</el-button>
          <el-button
              type="primary"
              @click="submitForm(formRef)"
              :loading="isSubmitting"
              size="large"
          >
            변경하기
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
  width: 220px;
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
  margin: 0 0 12px;
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
  margin: 0 0 24px;
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--el-text-color-primary);
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
  margin: 44px 0 8px;
  font-weight: 600;
}

.security-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px;
  border-radius: 8px;
  background-color: var(--el-fill-color-lighter);
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
  border-top: 1px solid var(--el-border-color-light);
}
</style>