<!-- src/components/StoreFormDialog.vue -->
<script setup lang="ts">
import { ref, watch, nextTick } from 'vue';
import { ElMessage, FormInstance, FormRules } from 'element-plus';
import {Shop, CollectionTag, Calendar, EditPen, Star, Check, FolderOpened, StarFilled, Link} from '@element-plus/icons-vue'; // Link 아이콘 추가
import FileUpload from '@/components/fileUpload/FileUpload.vue';
import {Api} from "@/api/axiosInstance";
import {ApiUrls} from "@/api/apiUrls";
import {userStore} from "@/store/userStore";

const userStoreObj = userStore();

// --- Props & Emits ---
const props = defineProps({
  visible: { type: Boolean, required: true },
  isEditMode: { type: Boolean, default: false },
  initialData: {
    type: Object,
    // ★★★★★ 수정된 부분: referenceUrl 속성 추가 ★★★★★
    default: () => ({ id: null, name: '', category: '', rating: 0, visitDate: new Date().toISOString().split('T')[0], memo: '', referenceUrl: '' }),
  },
});
const emit = defineEmits(['update:visible', 'submit']);

// --- 내부 상태 및 로직 ---
const formRef = ref<FormInstance>();
const formData = ref({ ...props.initialData });
const fileUploadRef = ref<InstanceType<typeof FileUpload> | null>(null);
const isSubmitting = ref(false);

watch(() => props.initialData, (newData) => {
  formData.value = { ...newData };
  fileUploadRef.value?.clearFiles();
  nextTick(() => { formRef.value?.clearValidate(); });
}, { deep: true });

// --- 유효성 검사 규칙 ---
const rules: FormRules = {
  name: [{ required: true, message: '가게 이름을 입력해주세요.', trigger: 'blur' }],
  category: [{ required: true, message: '카테고리를 입력해주세요.', trigger: 'blur' }],
  visitDate: [{ required: true, message: '방문 날짜를 선택해주세요.', trigger: 'change' }],
  rating: [{ required: true, message: '별점을 선택해주세요.', trigger: 'change', type: 'number', min: 1, message: '별점은 최소 1점 이상이어야 합니다.' }],
  // ★★★★★ 추가된 부분: URL 유효성 검사 ★★★★★
  referenceUrl: [{ type: 'url', message: '올바른 URL 형식으로 입력해주세요.', trigger: 'blur' }],
};

const handleClose = () => { emit('update:visible', false); };

// --- handleSubmit 로직 (기존과 동일) ---
const handleSubmit = async () => {
  if (!formRef.value || !fileUploadRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (valid) {
      isSubmitting.value = true;
      try {
        // 1. FileUpload 컴포넌트의 submit을 호출하여 파일들을 먼저 업로드
        //    성공 시, 서버로부터 받은 파일 정보 객체들의 배열을 반환받음
        const uploadedFileResponses = await fileUploadRef.value.submit() as any[];
        console.log('uploadedFileResponses', uploadedFileResponses);

        // 2. 최종적으로 서버에 전송할 페이로드(JSON)를 구성
        const finalPayload = {
          // 폼 데이터
          name: formData.value.name,
          category: formData.value.category,
          visitDate: formData.value.visitDate,
          rating: formData.value.rating,
          memo: formData.value.memo,
          referenceUrl: formData.value.referenceUrl,
          userId: userStoreObj.getUserInfo.userId,

          // ▼▼▼▼▼▼▼ 여기를 이렇게 바꾸세요 ▼▼▼▼▼▼▼
          images: uploadedFileResponses.map(response => {
            // uploadedFileResponses 배열의 각 요소는 { header: ..., data: ... } 형태의 객체입니다.
            // 이 객체를 'response'라고 부릅시다.
            // 우리가 필요한 파일 정보는 'response' 객체 안의 'data' 객체에 있습니다.
            const fileData = response.data; // data 객체를 변수로 빼서 가독성을 높입니다.

            return {
              originalFileName: fileData.originalFileName, // data 객체의 originalFileName 필드 사용
              storedFileName: fileData.storedFileName,     // data 객체의 storedFileName 필드 사용
              imageUrl: fileData.fileDownloadUri,        // data 객체의 fileDownloadUri 필드 사용
              fileSize: fileData.size,                   // data 객체의 size 필드 사용
            };
          }),
        };

        console.log('Final Payload to be sent:', finalPayload);

        // 3. 구성된 최종 페이로드를 새로운 API로 전송
        await Api.post(ApiUrls.CREATE_GOURMET_RECORD, finalPayload, true); // 로딩 옵션 사용

        ElMessage.success('맛집 기록이 성공적으로 저장되었습니다!');
        emit('submit', finalPayload); // 성공 이벤트 발생 (부모 컴포넌트 알림)
        handleClose(); // 다이얼로그 닫기

      } catch (error) {
        // 파일 업로드 실패 또는 최종 데이터 전송 실패 시
        ElMessage.error('기록 저장 중 오류가 발생했습니다. 다시 시도해 주세요.');
        console.error("Submission failed:", error);
      } finally {
        isSubmitting.value = false;
      }
    } else {
      ElMessage.warning('입력 항목을 모두 올바르게 채워주세요.');
    }
  });
};
</script>

<template>
  <Transition name="dialog-fade">
    <el-dialog
        v-if="visible"
        :model-value="visible"
        :show-close="false"
        width="700px"
        class="store-form-dialog"
        top="12vh"
        @close="handleClose"
        append-to-body
        :close-on-click-modal="false"
    >
      <template #header="{ close }">
        <div class="dialog-header-custom">
          <!-- 헤더 내용은 기존과 동일 -->
          <div class="header-main-content">
            <div class="header-icon">
              <el-icon v-if="isEditMode" :size="24"><EditPen /></el-icon>
              <el-icon v-else :size="24"><Shop /></el-icon>
            </div>
            <div class="header-title-group">
              <h2 class="header-title">{{ isEditMode ? '맛집 기록 수정' : '새로운 맛집 기록' }}</h2>
              <p class="header-subtitle">소중한 미식 경험을 기록하고 공유해보세요.</p>
            </div>
          </div>
          <el-button text circle class="close-btn" @click="close">✕</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="formData" :rules="rules" label-position="top" class="store-form">
        <!-- 1. 가게 이름 -->
        <el-form-item prop="name">
          <el-input
              v-model="formData.name"
              placeholder="가게 이름"
              :prefix-icon="Shop"
              size="large"
              autofocus
              maxlength="30"
              show-word-limit
          />
        </el-form-item>

        <!-- ★★★★★ 추가된 부분: 참조 URL ★★★★★ -->
        <el-form-item prop="referenceUrl">
          <el-input
              v-model="formData.referenceUrl"
              placeholder="참조 URL (예: 네이버 지도, 블로그 등)"
              :prefix-icon="Link"
              size="large"
              clearable
          />
        </el-form-item>

        <!-- 2. 카테고리, 방문일, 별점 그룹 -->
        <el-row :gutter="24">
          <el-col :span="6">
            <el-form-item prop="visitDate">
              <el-date-picker v-model="formData.visitDate" type="date" placeholder="방문일" format="YYYY-MM-DD" value-format="YYYY-MM-DD" :prefix-icon="Calendar" size="large" style="width: 100%;"/>
            </el-form-item>
          </el-col>
          <el-col :span="9">
            <el-form-item prop="category">
              <el-input v-model="formData.category" placeholder="카테고리" :prefix-icon="CollectionTag" size="large"/>
            </el-form-item>
          </el-col>
          <el-col :span="9">
            <el-form-item prop="rating">
              <div class="rating-wrapper">
                <el-icon class="prefix-icon"><StarFilled /></el-icon>
                <el-rate v-model="formData.rating" :max="5" class="custom-rate"/>
                <span class="rating-text">{{ formData.rating }} / 5</span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 3. 파일 업로드 -->
        <el-form-item prop="files">
          <FileUpload
              ref="fileUploadRef"
              :upload-url="'http://localhost:8080/api/files/upload'"
              :limit="10"
              :max-size-m-b="10"
              :initial-files="formData.images || []"
          />
        </el-form-item>

        <!-- 4. 한 줄 메모 -->
        <el-form-item prop="memo">
          <el-input
              v-model="formData.memo"
              type="textarea"
              :rows="4"
              placeholder="메모 (선택 사항)"
              resize="none"
              maxlength="500"
              show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleClose">취소</el-button>
          <el-button type="primary" @click="handleSubmit" :icon="Check" :loading="isSubmitting">
            {{ isEditMode ? '수정 완료' : '기록 저장' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </Transition>
</template>

<style scoped>
/* 모던 다이얼로그 스타일 */
.store-form-dialog :deep(.el-dialog__header) {
  padding: 0;
  margin-right: 0;
}
.dialog-header-custom {
  display: flex;
  justify-content: space-between;
  padding: 12px 0 12px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.header-main-content {
  display: flex;
  align-items: center;
  gap: 16px;
}
.header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.header-title-group .header-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--el-text-color-primary);
  font-family: 'Poppins', sans-serif;
  margin: 0;
}
.header-title-group .header-subtitle {
  font-size: 0.9rem;
  color: var(--el-text-color-secondary);
  margin: 4px 0 0;
}
.close-btn {
  margin: 0;
  font-size: 1.2rem;
  color: var(--el-text-color-placeholder);
  --el-button-hover-text-color: var(--el-color-primary);
  --el-button-hover-bg-color: var(--el-color-primary-light-9);
}

/* 바디 */
.store-form-dialog :deep(.el-dialog__body) {
  padding: 8px 32px 24px 32px;
}

/* 폼 스타일 */
.store-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.store-form :deep(.el-form-item) {
  margin-bottom: 2px;
}

/* 입력창 스타일 전역 개선 */
:deep(.el-input__wrapper),
:deep(.el-textarea__inner),
.rating-wrapper {
  background-color: var(--el-fill-color-light) !important; /* lighter -> light 로 변경하여 깊이감 추가 */
  box-shadow: none !important;
  border-radius: 10px;
  border: 1px solid transparent;
  transition: background-color 0.2s, border-color 0.2s;
  padding: 0 15px;
}
:deep(.el-input__wrapper:hover),
:deep(.el-textarea__inner:hover),
.rating-wrapper:hover {
  border-color: var(--el-border-color);
}
:deep(.el-input__wrapper.is-focus),
:deep(.el-textarea__inner:focus-within) {
  background-color: var(--el-bg-color) !important;
  border-color: var(--el-color-primary) !important;
}

/* 파일 업로드 컴포넌트 배경색도 통일 */
.store-form :deep(.el-form-item .image-uploader-container) {
  background-color: var(--el-fill-color-light);
}
.rating-text {
  margin-left: auto;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-variant-numeric: tabular-nums;
}
:deep(.el-input .el-input__prefix) {
  color: var(--el-text-color-placeholder);
}
:deep(.el-date-editor .el-input__prefix .el-icon) {
  color: var(--el-text-color-placeholder);
}
:deep(.el-textarea__inner) {
  padding: 10px 15px;
}

/* 별점 입력을 위한 커스텀 래퍼 */
.rating-wrapper {
  display: flex;
  align-items: center;
  width: 100%;
  height: 40px;
  box-sizing: border-box;
}
.rating-wrapper .prefix-icon {
  font-size: 16px;
  color: var(--el-color-primary);
  margin-right: 8px;
}
.custom-rate {
  height: auto;
  --el-rate-font-size: 24px;
}

/* 푸터 */
.store-form-dialog :deep(.el-dialog__footer) {
  padding: 20px 32px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}

/* 다이얼로그 등장 애니메이션 */
.dialog-fade-enter-active, .dialog-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.dialog-fade-enter-from, .dialog-fade-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(20px);
}

.store-form :deep(.el-form-item) {
  margin-bottom: 3px;
}

/* (추천) 유효성 검사 실패 시 입력 필드 테두리 색상 변경 */
.store-form :deep(.el-form-item.is-error .el-input__wrapper),
.store-form :deep(.el-form-item.is-error .el-textarea__inner),
.store-form :deep(.el-form-item.is-error .rating-wrapper) {
  border-color: var(--el-color-danger);
}

/* 에러 메시지 자체의 스타일 */
.store-form :deep(.el-form-item__error) {
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 1.2;
  position: absolute;
  top: 100%;
  left: 5px; /* 왼쪽 여백을 살짝 주어 보기 좋게 정렬 */
  padding-top: 4px; /* margin-top 대신 padding-top을 사용하여 안정적으로 공간 확보 */
}
:deep(.el-input .el-input__count .el-input__count-inner),
:deep(.el-textarea .el-input__count .el-input__count-inner) {
  background-color: var(--el-fill-color-light) !important; /* 배경색을 입력 필드와 동일하게 설정 */
  padding-left: 8px;
}
:deep(.el-textarea .el-input__count) {
  background-color: var(--el-fill-color-light) !important; /* 배경색을 입력 필드와 동일하게 설정 */
  padding-left: 8px;
}

</style>