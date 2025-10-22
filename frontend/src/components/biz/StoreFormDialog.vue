<!-- src/components/StoreFormDialog.vue -->
<script setup lang="ts">
import { ref, watch, nextTick } from 'vue';
import { ElMessage, FormInstance, FormRules } from 'element-plus';
import { Shop, CollectionTag, Calendar, EditPen, Check, StarFilled, Link, Upload, Delete } from '@element-plus/icons-vue';
import draggable from 'vuedraggable';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { userStore } from "@/store/userStore";
import { Dialogs } from "@/common/dialogs";

// FileUpload.vue에서 반환하는 파일 정보 타입 (FileResponse와 동일)
interface UploadedFileResponse {
  originalFileName: string;
  storedFileName: string; // 임시 저장된 파일명 (예: 'uuid-1234.jpg')
  fileDownloadUri: string;
  size: number;
}

// 서버의 GourmetImageDto와 형식을 맞춘 타입
interface GourmetImage {
  imageId: number | null; // 새 이미지는 null
  originalFileName: string;
  storedFileName: string;
  imageUrl: string;
  fileSize: number;
  imageOrder: number;
}

const userStoreObj = userStore();

const props = defineProps({
  visible: { type: Boolean, required: true },
  isEditMode: { type: Boolean, default: false },
  initialData: {
    type: Object,
    default: () => ({ id: null, name: '', category: '', rating: 0, visitDate: new Date().toISOString().split('T')[0], memo: '', referenceUrl: '', images: [] }),
  },
});
const emit = defineEmits(['update:visible', 'submit']);

const formRef = ref<FormInstance>();
const formData = ref({ ...props.initialData });
const isSubmitting = ref(false);
const isUploading = ref(false);

watch(() => props.visible, (isVisible) => {
  if (isVisible) {
    // initialData를 기반으로 formData를 설정합니다.
    const newData = props.initialData;

    formData.value.id = newData.id || null;
    formData.value.name = newData.name || '';
    formData.value.category = newData.category || '';
    formData.value.rating = newData.rating || 0;
    formData.value.memo = newData.memo || '';
    formData.value.referenceUrl = newData.referenceUrl || '';

    if (newData.visitDate && typeof newData.visitDate === 'string') {
      formData.value.visitDate = newData.visitDate.split('T')[0];
    } else {
      formData.value.visitDate = new Date().toISOString().split('T')[0];
    }

    // images 배열은 항상 새로 할당하여 이전 상태가 남지 않도록 합니다.
    formData.value.images = newData.images ? [...newData.images] : [];

    // 폼 유효성 검사 상태를 초기화합니다.
    nextTick(() => {
      formRef.value?.clearValidate();
    });
  }
}, { deep: true, immediate: true });

const rules: FormRules = {
  name: [{ required: true, message: '가게 이름을 입력해주세요.', trigger: 'blur' }],
  category: [{ required: true, message: '카테고리를 입력해주세요.', trigger: 'blur' }],
  visitDate: [{ required: true, message: '방문 날짜를 선택해주세요.', trigger: 'change' }],
  rating: [{ required: true, message: '별점은 최소 1점 이상이어야 합니다.', trigger: 'change', type: 'number', min: 1 }],
  referenceUrl: [{ type: 'url', message: '올바른 URL 형식으로 입력해주세요.', trigger: 'blur' }],
};

const handleClose = () => { emit('update:visible', false); };

const uploadUrl = 'http://localhost:8080/api/files/upload';
const maxFiles = 10;

const handleFileSelect = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (!input.files) return;

  const filesToUpload = Array.from(input.files);
  if (filesToUpload.length === 0) return;

  isUploading.value = true;
  try {
    for (const file of filesToUpload) {
      if (formData.value.images.length >= maxFiles) {
        ElMessage.warning(`이미지는 최대 ${maxFiles}개까지만 추가할 수 있습니다.`);
        break;
      }
      if (file.size > 10 * 1024 * 1024) {
        ElMessage.warning(`'${file.name}' 파일의 크기가 10MB를 초과하여 업로드할 수 없습니다.`);
        continue;
      }
      await uploadFile(file);
    }
  } finally {
    isUploading.value = false;
    input.value = '';
  }
};

const uploadFile = async (file: File) => {
  const uploadData = new FormData();
  uploadData.append('file', file);
  try {
    const response = await Api.post(uploadUrl, uploadData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    const newImage = response.data; // 서버로부터 받은 FileResponse 객체

    const imageObjectToAdd: GourmetImage = {
      imageId: null, // 새 이미지이므로 ID는 null
      originalFileName: newImage.originalFileName,
      storedFileName: newImage.storedFileName,
      imageUrl: newImage.imageUrl || newImage.fileDownloadUri,
      fileSize: newImage.size || newImage.fileSize,
      imageOrder: formData.value.images.length, // 현재 배열 길이를 순서로 사용
    };

    formData.value.images.push(imageObjectToAdd);

    console.log(formData.value)

  } catch (error) {
    console.error(`File upload failed for ${file.name}:`, error);
  }
};

const removeImage = (index: number) => {
  formData.value.images.splice(index, 1);
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      isSubmitting.value = true;
      try {

        await Dialogs.customConfirm(
            '저장소 기록',
            '기록을 등록하시겠습니까?',
            '등록하기',
            '취소',
            '420px',
        );

        const finalPayload = {
          recordId: formData.value.id,
          name: formData.value.name,
          category: formData.value.category,
          visitDate: formData.value.visitDate,
          rating: formData.value.rating,
          memo: formData.value.memo,
          referenceUrl: formData.value.referenceUrl,
          userId: userStoreObj.getUserInfo.userId,
          images: formData.value.images.map((image: any, index: any) => ({
            imageId: image.imageId || null, // 기존 이미지의 ID를 전달
            originalFileName: image.originalFileName,
            storedFileName: image.storedFileName,
            imageUrl: image.imageUrl,
            fileSize: image.fileSize,
            imageOrder: index, // 순서 정보도 함께 전달
          })),
        };
        await Api.post(ApiUrls.CREATE_GOURMET_RECORD, finalPayload);
        ElMessage.success('맛집 기록이 성공적으로 저장되었습니다!');
        emit('submit', finalPayload);
        handleClose();
      } catch (error) {
        if (error !== 'cancel') {
          console.error("Submission failed:", error);
          ElMessage.error('저장에 실패했습니다. 다시 시도해주세요.');
        }
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
          <div class="header-main-content">
            <div class="header-icon"><el-icon v-if="isEditMode" :size="24"><EditPen /></el-icon><el-icon v-else :size="24"><Shop /></el-icon></div>
            <div class="header-title-group">
              <h2 class="header-title">{{ isEditMode ? '맛집 기록 수정' : '새로운 맛집 기록' }}</h2>
              <p class="header-subtitle">소중한 미식 경험을 기록하고 공유해보세요.</p>
            </div>
          </div>
          <el-button text circle class="close-btn" @click="close">✕</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="formData" :rules="rules" label-position="top" class="store-form">
        <el-form-item prop="name"><el-input v-model="formData.name" placeholder="가게 이름" :prefix-icon="Shop" size="large" autofocus maxlength="30" show-word-limit/></el-form-item>
        <el-form-item prop="referenceUrl"><el-input v-model="formData.referenceUrl" placeholder="참조 URL (예: 네이버 지도, 블로그 등)" :prefix-icon="Link" size="large" clearable/></el-form-item>
        <el-row :gutter="24">
          <el-col :span="6"><el-form-item prop="visitDate"><el-date-picker v-model="formData.visitDate" type="date" placeholder="방문일" format="YYYY-MM-DD" value-format="YYYY-MM-DD" :prefix-icon="Calendar" size="large" style="width: 100%;"/></el-form-item></el-col>
          <el-col :span="9"><el-form-item prop="category"><el-input v-model="formData.category" placeholder="카테고리" :prefix-icon="CollectionTag" size="large" maxlength="40" show-word-limit/></el-form-item></el-col>
          <el-col :span="9"><el-form-item prop="rating"><div class="rating-wrapper"><el-icon class="prefix-icon"><StarFilled /></el-icon><el-rate v-model="formData.rating" :max="5" class="custom-rate"/><span class="rating-text">{{ formData.rating }} / 5</span></div></el-form-item></el-col>
        </el-row>

        <el-form-item prop="files">
          <div class="image-uploader-container">
            <div class="uploader-header">
              <span class="uploader-title">사진</span>
              <span class="image-counter">{{ formData.images.length }} / {{ maxFiles }}</span>
            </div>
            <div class="scroll-wrapper">
              <label for="file-upload-input" class="upload-box" v-if="formData.images.length < maxFiles">
                <div v-if="isUploading" class="upload-loading"><el-icon class="is-loading" :size="24"><Upload /></el-icon><span>업로드 중..</span></div>
                <div v-else class="upload-content"><el-icon :size="24"><Upload /></el-icon><span>이미지 추가</span></div>
              </label>
              <draggable
                  v-model="formData.images"
                  item-key="storedFileName"
                  class="image-list-inline"
                  ghost-class="ghost"
              >
                <template #item="{ element, index }">
                  <div class="image-preview-item">
                    <img :src="element.imageUrl" :alt="element.originalFileName" class="preview-image"/>
                    <div class="image-overlay">
                      <span class="image-name">{{ element.originalFileName }}</span>
                      <el-button type="danger" :icon="Delete" circle class="delete-btn" @click.stop="removeImage(index)"/>
                    </div>
                  </div>
                </template>
              </draggable>
            </div>
            <input type="file" id="file-upload-input" @change="handleFileSelect" accept="image/*" multiple hidden/>
          </div>
        </el-form-item>

        <el-form-item prop="memo"><el-input v-model="formData.memo" type="textarea" :rows="4" placeholder="메모 (선택 사항)" resize="none" maxlength="500" show-word-limit/></el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleClose">취소</el-button>
          <el-button type="primary" @click="handleSubmit" :icon="Check" :loading="isSubmitting">{{ isEditMode ? '수정 완료' : '기록 저장' }}</el-button>
        </div>
      </template>
    </el-dialog>
  </Transition>
</template>

<style scoped>
/* --- 이미지 업로더 (Image Uploader) --- */

/* 전체 업로더 컨테이너 */
.image-uploader-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  background-color: var(--el-fill-color-light);
  border-radius: 8px;
  padding: 12px;
  border: 1px solid transparent;
  transition: border-color 0.2s;
}
.image-uploader-container:hover {
  border-color: var(--el-border-color);
}

/* 업로더 내부 헤더 (타이틀, 카운터) */
.uploader-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 0 4px;
}
.uploader-title {
  font-size: 14px;
  color: var(--el-text-color-regular);
  font-weight: 600;
}
.image-counter {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
  margin-right: 4px;
}

/* 가로 스크롤 영역 */
.scroll-wrapper {
  overflow-x: scroll; /* auto 대신 scroll을 사용하여 높이 변동 방지 */
  overflow-y: hidden;
  padding: 4px 0 12px 0;
  white-space: nowrap;
  font-size: 0; /* inline-block 사이의 불필요한 여백 제거 */
  max-width: 641px;
}
.scroll-wrapper::-webkit-scrollbar {
  height: 6px;
}
.scroll-wrapper::-webkit-scrollbar-track {
  background: transparent;
}
.scroll-wrapper::-webkit-scrollbar-thumb {
  background-color: var(--el-border-color);
  border-radius: 4px;
}

/* Draggable 컴포넌트 래퍼 */
.image-list-inline {
  display: contents; /* 부모의 레이아웃 규칙을 따름 */
}

/* 이미지 썸네일 & '이미지 추가' 버튼 공통 스타일 */
.image-preview-item, .upload-box {
  display: inline-block;
  vertical-align: top;
  width: 120px;
  height: 120px;
  border-radius: 8px;
  position: relative;
  overflow: hidden;
  background-color: var(--el-border-color-lighter);
  margin-right: 12px; /* gap 대신 사용 */
}
.image-preview-item:last-child, .upload-box:last-child {
  margin-right: 0;
}

/* 개별 이미지 썸네일 스타일 */
.image-preview-item {
  cursor: grab;
}
.image-preview-item:active {
  cursor: grabbing;
  transform: scale(0.95);
  box-shadow: 0 4px 15px rgba(0,0,0,0.15);
}
.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}
.image-preview-item:hover .preview-image {
  transform: scale(1.1);
}

/* 이미지 호버 시 나타나는 오버레이 */
.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(to top, rgba(0,0,0,0.6) 0%, rgba(0,0,0,0) 40%);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 8px;
  box-sizing: border-box;
  opacity: 0;
  transition: opacity 0.3s ease;
  color: white;
}
.image-preview-item:hover .image-overlay {
  opacity: 1;
}
.image-name {
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}
.delete-btn {
  position: absolute;
  top: 6px;
  right: 6px;
  --el-button-size: 24px;
  background-color: rgba(0,0,0,0.4);
  border: none;
}
.delete-btn:hover {
  background-color: var(--el-color-danger);
}

/* '이미지 추가' 버튼 스타일 */
.upload-box {
  border: 2px dashed var(--el-border-color);
  cursor: pointer;
  transition: all 0.2s;
  color: var(--el-text-color-placeholder);
}
.upload-box:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}
.upload-content, .upload-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  width: 100%;
  height: 100%;
}
.upload-loading span {
  font-size: 12px;
}

/* 드래그 시 나타나는 placeholder 스타일 */
.ghost {
  opacity: 0.5;
  background: var(--el-color-primary-light-8);
  border: 2px dashed var(--el-color-primary);
}


/* --- 다이얼로그 레이아웃 (Dialog Layout) --- */

/* 다이얼로그 헤더 */
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

/* 다이얼로그 바디 */
.store-form-dialog :deep(.el-dialog__body) {
  padding: 8px 32px 24px 32px;
}

/* 다이얼로그 푸터 */
.store-form-dialog :deep(.el-dialog__footer) {
  padding: 20px 32px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}


/* --- 폼 & 입력 요소 (Form & Input Elements) --- */

.store-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.store-form :deep(.el-form-item) {
  margin-bottom: 3px;
}

/* 별점(Rating) 입력 래퍼 */
.rating-wrapper {
  background-color: var(--el-fill-color-light) !important;
  box-shadow: none !important;
  border-radius: 10px;
  border: 1px solid transparent;
  transition: background-color 0.2s, border-color 0.2s;
  padding: 0 15px;
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
.rating-text {
  margin-left: auto;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-variant-numeric: tabular-nums;
}

/* 입력 필드 공통 호버/포커스 스타일 */
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

/* 아이콘 및 텍스트 스타일 */
:deep(.el-input .el-input__prefix),
:deep(.el-date-editor .el-input__prefix .el-icon) {
  color: var(--el-text-color-placeholder);
}
:deep(.el-textarea__inner) {
  padding: 10px 15px;
}

/* 글자 수 카운터 스타일 */
:deep(.el-input .el-input__count .el-input__count-inner),
:deep(.el-textarea .el-input__count .el-input__count-inner),
:deep(.el-textarea .el-input__count) {
  background-color: var(--el-bg-color) !important;
  padding-left: 8px;
}


/* --- 유효성 검사 & 애니메이션 (Validation & Animation) --- */

/* 다이얼로그 등장 애니메이션 */
.dialog-fade-enter-active, .dialog-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.dialog-fade-enter-from, .dialog-fade-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(20px);
}

/* 유효성 검사 에러 스타일 */
.store-form :deep(.el-form-item.is-error .el-input__wrapper),
.store-form :deep(.el-form-item.is-error .el-textarea__inner),
.store-form :deep(.el-form-item.is-error .rating-wrapper) {
  border-color: var(--el-color-danger);
}
.store-form :deep(.el-form-item__error) {
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 1.2;
  position: absolute;
  top: 100%;
  left: 5px;
  padding-top: 4px;
}
</style>