<!-- src/components/FileUpload.vue -->
<template>
  <div class="image-uploader-container">
    <!-- 1. 헤더: 제목 및 파일 개수 카운터 -->
    <div class="uploader-header">
      <span class="header-title">사진 첨부</span>
      <span class="header-info">{{ fileList.length }} / {{ props.limit }}</span>
    </div>

    <!-- 2. 가로 스크롤 영역 -->
    <div class="image-list-wrapper">
      <div class="image-list">
        <!-- 사진 추가 버튼 카드 (파일 제한에 도달하지 않았을 때만 보임) -->
        <div
            v-if="fileList.length < props.limit"
            class="add-photo-trigger"
            @click="triggerUpload"
        >
          <el-icon><CameraFilled /></el-icon>
          <span>사진 추가</span>
        </div>

        <!-- 선택된 이미지 미리보기 카드 -->
        <transition-group name="image-preview-anim">
          <!-- ★★★★★ 수정된 구조: 이미지를 감싸는 새 래퍼 추가 ★★★★★ -->
          <div
              v-for="(file, index) in fileList"
              :key="file.uid"
              class="image-card-wrapper"
          >
            <!-- 이미지 자체를 담는 컨테이너 (overflow: hidden 적용) -->
            <div class="image-preview-item">
              <el-image
                  v-if="file.url"
                  class="preview-image"
                  :src="file.url"
                  :preview-src-list="previewSrcList"
                  :initial-index="index"
                  fit="cover"
                  hide-on-click-modal
              />
              <!-- 업로드 진행률 오버레이 -->
              <div v-if="file.status === 'uploading'" class="upload-progress-overlay">
                <el-progress type="circle" :percentage="file.percentage || 0" :width="40" color="#fff"/>
              </div>
            </div>
            <!-- 삭제 버튼 (이제 래퍼를 기준으로 위치하여 잘리지 않음) -->
            <el-button
                class="remove-button"
                type="danger"
                :icon="CloseBold"
                circle
                @click="handleRemove(file)"
            />
          </div>
        </transition-group>
      </div>
    </div>

    <!-- 실제 파일 입력을 담당하는 숨겨진 el-upload 컴포넌트 -->
    <el-upload
        ref="uploadRef"
        class="hidden-uploader"
        :action="props.uploadUrl"
        :limit="props.limit"
        :on-exceed="handleExceed"
        :on-change="handleChange"
        :on-remove="handleRemoveInternal"
        :before-upload="beforeUpload"
        :on-success="handleSuccess"
        :on-error="handleError"
        :file-list="fileList"
        :auto-upload="false"
        :show-file-list="false"
        accept="image/*"
        multiple
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { CameraFilled, CloseBold } from '@element-plus/icons-vue';
import type { UploadInstance, UploadProps, UploadRawFile, UploadUserFile, UploadFile, UploadFiles } from 'element-plus';
import { ApiUrls } from "@/api/apiUrls";
import { Api } from "@/api/axiosInstance";

// --- Props & Emits ---
interface Props {
  uploadUrl?: string;
  limit?: number;
  maxSizeMB?: number;
  allowedExtensions?: string[];
  initialFiles?: any[];
}

const props = withDefaults(defineProps<Props>(), {
  uploadUrl: 'http://localhost:8080/api/files/upload',
  limit: 10,
  maxSizeMB: 10,
  allowedExtensions: () => ['jpg', 'jpeg', 'png', 'gif', 'webp'],
  initialFiles: () => [],
});

interface UploadSuccessResponse {
  fileName: string;
  fileDownloadUri: string;
  originalFileName: string;
  storedFileName: string;
  imageUrl: string;
  size: number;
}
const emit = defineEmits<{
  (e: 'upload-success', data: { response: UploadSuccessResponse; file: UploadFile; fileList: UploadFiles }): void;
  (e: 'upload-error', data: { error: Error; file: UploadFile; fileList: UploadFiles }): void;
  (e: 'file-list-changed', fileList: UploadUserFile[]): void;
}>();


// --- Refs & Computed ---
const uploadRef = ref<UploadInstance>();
const fileList = ref<UploadUserFile[]>([]);

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

watch(() => props.initialFiles, (newFiles) => {
  if (newFiles && newFiles.length > 0 && fileList.value.length === 0) {
    fileList.value = newFiles.map((file, index) => ({
      name: file.originalFileName,
      url: file.imageUrl,
      status: 'success',
      uid: Date.now() + index,
      // ★★★★★ 수정된 부분: response에 원본 데이터 '그대로' 저장 (불필요한 data 객체 래핑 제거) ★★★★★
      response: file
    }));
  }
}, { immediate: true });


const previewSrcList = computed(() =>
    fileList.value.map(file => file.url).filter(Boolean) as string[]
);

const triggerUpload = () => {
  uploadRef.value?.$el.querySelector('input')?.click();
};

const handleChange: UploadProps['onChange'] = (uploadFile, uploadFiles) => {
  if (uploadFile.status === 'ready' && uploadFile.raw && uploadFile.raw.type.startsWith('image/')) {
    uploadFile.url = URL.createObjectURL(uploadFile.raw);
  }
  fileList.value = uploadFiles;
};

const handleRemove = (file: UploadUserFile) => {
  uploadRef.value?.handleRemove(file);
};

const handleRemoveInternal: UploadProps['onRemove'] = (file, files) => {
  if (file.url && file.url.startsWith('blob:')) {
    URL.revokeObjectURL(file.url);
  }
  fileList.value = files;
};

const handleClearAll = () => {
  fileList.value.forEach(file => {
    if (file.url && file.url.startsWith('blob:')) URL.revokeObjectURL(file.url);
  });
  uploadRef.value?.clearFiles();
};

onUnmounted(() => {
  handleClearAll();
});

const handleExceed: UploadProps['onExceed'] = () => {
  ElMessage.warning(`최대 ${props.limit}개의 사진만 첨부할 수 있습니다.`);
};

const beforeUpload: UploadProps['beforeUpload'] = (rawFile: UploadRawFile) => {
  const fileExtension = rawFile.name.substring(rawFile.name.lastIndexOf('.') + 1).toLowerCase();
  if (!props.allowedExtensions.includes(fileExtension)) {
    ElMessage.error(`'${fileExtension}' 확장자는 허용되지 않습니다.`);
    return false;
  }
  if (rawFile.size / 1024 / 1024 > props.maxSizeMB) {
    ElMessage.error(`사진 크기는 ${props.maxSizeMB}MB를 초과할 수 없습니다!`);
    return false;
  }
  return true;
};

const handleSuccess: UploadProps['onSuccess'] = (response, uploadFile, uploadFiles) => {
  fileList.value = uploadFiles;
  emit('upload-success', { response, file: uploadFile, fileList: uploadFiles });
};

const handleError: UploadProps['onError'] = (error, uploadFile, uploadFiles) => {
  console.error("el-upload onError:", { error, uploadFile });
  let errorMessage = '파일 업로드 실패';
  try {
    const errorResponse = JSON.parse(error.message);
    if (errorResponse?.message) errorMessage = errorResponse.message;
  } catch (e) {}
  ElMessage.error(`${uploadFile.name}: ${errorMessage}`);
  emit('upload-error', { error, file: uploadFile, fileList: uploadFiles });
};


// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
// ★★★★★ 핵심 수정: submit 함수를 Promise.allSettled로 변경하여 안정성 확보 ★★★★★
// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
const submit = () => {
  return new Promise(async (resolve, reject) => {
    // 1. 이미 업로드된 파일과 새로 업로드할 파일 분리
    const alreadyUploadedFiles = fileList.value
        .filter(file => file.status === 'success')
        .map(file => file.response); // response에는 원본 데이터가 저장되어 있음

    const filesToUpload = fileList.value.filter(file => file.status === 'ready');

    console.log(`[FileUpload] 기존 파일 ${alreadyUploadedFiles.length}개`, alreadyUploadedFiles);
    console.log(`[FileUpload] 새로 업로드할 파일 ${filesToUpload.length}개`, filesToUpload);


    // 2. 새로 업로드할 파일이 없으면 기존 파일 목록만 반환
    if (filesToUpload.length === 0) {
      console.log('[FileUpload] 새로 업로드할 파일 없음. 기존 파일 목록만 반환.');
      resolve(alreadyUploadedFiles);
      return;
    }

    // 3. 업로드 Promise 배열 생성
    const uploadPromises = filesToUpload.map(file => {
      const formData = new FormData();
      formData.append('file', file.raw as Blob);
      console.log(`[FileUpload] '${file.name}' 업로드 시작...`);
      // Api.post는 AxiosPromise를 반환합니다.
      return Api.post(ApiUrls.FILE_UPLOAD, formData, false);
    });

    try {
      // 4. Promise.allSettled를 사용하여 모든 업로드가 완료될 때까지 대기
      const results = await Promise.allSettled(uploadPromises);
      console.log('[FileUpload] 모든 업로드 시도 완료. 결과:', results);

      const successfulUploads: any[] = [];
      const failedUploads: any[] = [];

      // 5. 성공/실패 결과 분리
      results.forEach((result, index) => {
        const originalFile = filesToUpload[index];
        if (result.status === 'fulfilled') {
          console.log(`[FileUpload] '${originalFile.name}' 업로드 성공.`, result.value);
          // 성공한 경우, 서버 응답 데이터(result.value)를 저장
          successfulUploads.push(result.value);
        } else {
          console.error(`[FileUpload] '${originalFile.name}' 업로드 실패. 원인:`, result.reason);
          failedUploads.push({
            fileName: originalFile.name,
            reason: result.reason,
          });
        }
      });

      // 6. 실패한 업로드가 하나라도 있으면 전체 Promise를 reject 처리
      if (failedUploads.length > 0) {
        const failedFileNames = failedUploads.map(f => f.fileName).join(', ');
        ElMessage.error(`${failedFileNames} 파일 업로드에 실패했습니다.`);
        // 실패에 대한 상세 정보를 reject로 넘겨 부모 컴포넌트에서 확인할 수 있도록 함
        reject({
          message: '하나 이상의 파일 업로드에 실패했습니다.',
          failedFiles: failedUploads,
        });
        return;
      }

      // 7. 모든 파일이 성공한 경우, 기존 파일과 새로 업로드된 파일 목록을 합쳐서 반환
      const allFiles = [...alreadyUploadedFiles, ...successfulUploads];
      console.log('[FileUpload] 모든 파일 처리 성공. 최종 목록:', allFiles);
      resolve(allFiles);

    } catch (error) {
      // Promise.allSettled 자체는 에러를 throw하지 않지만, 예외 상황을 대비한 catch 블록
      ElMessage.error('파일 업로드 중 예기치 않은 오류가 발생했습니다.');
      console.error("[FileUpload] 예기치 않은 오류:", error);
      reject(error);
    }
  });
};


defineExpose({ submit, clearFiles: handleClearAll, getFiles: () => fileList.value });
</script>

<style scoped>
/* CSS는 변경 사항이 없으므로 기존 코드를 그대로 사용합니다. */
.image-uploader-container {
  width: 100%;
  background-color: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 16px;
  box-sizing: border-box;
}

/* 헤더 */
.uploader-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 16px;
}
.header-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.header-info {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 가로 스크롤 영역 */
.image-list-wrapper {
  overflow-x: scroll; /* auto -> scroll로 변경하여 항상 스크롤바 공간을 차지하게 함 */
  overflow-y: hidden;
  scrollbar-width: thin;
  scrollbar-color: var(--el-border-color) transparent;
}
.image-list-wrapper::-webkit-scrollbar {
  height: 6px; /* 스크롤바의 높이 */
}
.image-list-wrapper::-webkit-scrollbar-thumb {
  background-color: var(--el-border-color);
  border-radius: 3px;
}
.image-list-wrapper::-webkit-scrollbar-track {
  background: transparent; /* 트랙 배경을 투명하게 하여 거슬리지 않게 함 */
}

/* 실제 이미지와 버튼이 담기는 flex 컨테이너 */
.image-list {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 0; /* X 버튼이 잘리지 않도록 상단에 여백 추가 */
  width: max-content;
}

/* 사진 추가 버튼 카드 */
.add-photo-trigger {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
  border: 2px dashed var(--el-border-color);
  color: var(--el-text-color-placeholder);
  cursor: pointer;
  transition: all 0.2s ease;
}
.add-photo-trigger:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}
.add-photo-trigger .el-icon {
  font-size: 32px;
}
.add-photo-trigger span {
  font-size: 0.9rem;
}

/* ★★★★★ 수정된 스타일 ★★★★★ */
/* 1. 이미지와 X버튼을 감싸는 새 래퍼 */
.image-card-wrapper {
  position: relative; /* 삭제 버튼의 기준점 */
  width: 120px;
  height: 120px;
  flex-shrink: 0;
}

/* 2. 이미지 자체를 담는 컨테이너 */
.image-preview-item {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  overflow: hidden; /* 이미지의 모서리를 둥글게 자르기 위해 필수 */
  background-color: var(--el-fill-color);
  position: relative; /* 프로그레스바 오버레이의 기준점 */
}

.preview-image {
  width: 100%;
  height: 100%;
}

/* 업로드 진행률 오버레이 */
.upload-progress-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 3. 삭제 버튼 (래퍼 기준으로 위치하여 잘리지 않음) */
.remove-button {
  position: absolute;
  top: -8px;
  right: -8px;
  z-index: 10;
  background-color: var(--el-color-danger) !important;
  color: white !important;
  border: 2px solid var(--el-bg-color-overlay);
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
  width: 24px;
  height: 24px;
}
.remove-button:hover {
  transform: scale(1.1);
}

/* 숨겨진 el-upload */
.hidden-uploader {
  display: none;
}

/* 애니메이션 */
.image-preview-anim-enter-active,
.image-preview-anim-leave-active {
  transition: all 0.3s cubic-bezier(0.55, 0, 0.1, 1);
}
.image-preview-anim-enter-from,
.image-preview-anim-leave-to {
  opacity: 0;
  transform: scale(0.5) translateY(30px);
}
</style>