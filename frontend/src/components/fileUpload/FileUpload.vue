<!-- src/components/FileUpload.vue -->
<template>
  <div class="compact-file-upload-container">
    <!-- 1. 파일 목록 (헤더 포함) -->
    <div class="file-list-wrapper">
      <div class="file-list-header">
        <span class="header-title">첨부파일 목록</span>
        <span class="header-info">
          {{ fileList.length }} / {{ props.limit }}개 (총 {{ totalSize }})
        </span>
      </div>

      <transition-group name="file-list-anim" tag="ul" class="compact-file-list">
        <!-- 파일이 없을 때 플레이스홀더 -->
        <li v-if="fileList.length === 0" key="placeholder" class="placeholder">
          <el-icon style="font-size: 30px;"><UploadFilled /></el-icon>
          <p>여기로 파일을 드래그하거나, 아래 버튼을 눌러 추가하세요.</p>
          <p class="placeholder-rules">
            ({{ props.allowedExtensions.join(', ') }} / 최대 {{ props.maxSizeMB }}MB)
          </p>
        </li>
        <!-- 파일 목록 아이템 -->
        <li v-for="file in fileList" :key="file.uid" class="compact-file-item">
          <div class="file-info">
            <el-icon><Document /></el-icon>
            <span class="file-name" :title="file.name">{{ file.name }}</span>
            <span class="file-size">({{ formatFileSize(file.size) }})</span>
          </div>
          <div class="file-status-actions">
            <div class="file-status">
              <el-progress v-if="file.status === 'uploading'" :percentage="file.percentage || 0" :stroke-width="4" :show-text="false" color="#67C23A" />
              <span v-else-if="file.status === 'success'" class="status-label success"><el-icon><CircleCheckFilled /></el-icon></span>
              <span v-else-if="file.status === 'fail'" class="status-label fail"><el-icon><CircleCloseFilled /></el-icon></span>
              <span v-else class="status-label ready"><el-icon><Clock /></el-icon></span>
            </div>
            <el-button type="danger" icon="Delete" style="font-size: 16px;" link @click="handleRemove(file)" />
          </div>
        </li>
      </transition-group>
    </div>

    <!-- 2. 업로드 트리거 (el-upload는 숨김) -->
    <el-upload
        ref="uploadRef"
        :action="props.uploadUrl"
        :limit="props.limit"
        :on-exceed="handleExceed"
        :on-change="handleChange"
        :before-upload="beforeUpload"
        :on-progress="handleProgress"
        :on-success="handleSuccess"
        :on-error="handleError"
        :file-list="fileList"
        :auto-upload="false"
        :show-file-list="false"
        multiple
    >
      <template #trigger>
        <div class="upload-actions">
          <el-button type="primary" icon="Upload">파일 선택</el-button>
          <el-button icon="DeleteFilled" v-if="fileList.length > 0" type="danger" plain @click.stop="handleClearAll">전체 삭제</el-button>
        </div>
      </template>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  UploadFilled,
  Document,
  CircleCheck,
  Close,
} from '@element-plus/icons-vue';
import type {
  UploadInstance,
  UploadProps,
  UploadRawFile,
  UploadUserFile,
  UploadFile,
  UploadFiles,
} from 'element-plus';

// --- Props & Emits ---
interface Props {
  uploadUrl?: string;
  limit?: number;
  maxSizeMB?: number;
  allowedExtensions?: string[];
}

const props = withDefaults(defineProps<Props>(), {
  uploadUrl: 'http://localhost:8080/api/files/upload',
  limit: 10,
  maxSizeMB: 10,
  allowedExtensions: () => ['jpg', 'jpeg', 'png', 'gif', 'pdf', 'zip', 'hwp', 'docx'],
});

interface UploadSuccessResponse {
  fileName: string;
  fileDownloadUri: string;
}

const emit = defineEmits<{
  (e: 'upload-success', data: { response: UploadSuccessResponse; file: UploadFile; fileList: UploadFiles }): void;
  (e: 'upload-error', data: { error: Error; file: UploadFile; fileList: UploadFiles }): void;
  (e: 'file-list-changed', fileList: UploadUserFile[]): void; // 파일 목록 변경 이벤트 추가
}>();

// --- Refs & Computed ---
const uploadRef = ref<UploadInstance>();
const fileList = ref<UploadUserFile[]>([]);

const totalSize = computed(() => {
  const total = fileList.value.reduce((sum, file) => sum + (file.size || 0), 0);
  return formatFileSize(total);
});

// --- 파일 크기 포맷팅 유틸 함수 ---
const formatFileSize = (size: number | undefined) => {
  if (!size) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let i = 0;
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024;
    i++;
  }
  return `${size.toFixed(1)} ${units[i]}`;
};

// --- Handlers ---
// 파일 선택/변경 시 Element Plus 내부 목록을 우리 ref와 동기화
const handleChange: UploadProps['onChange'] = (uploadFile, uploadFiles) => {
  fileList.value = uploadFiles;
  emit('file-list-changed', uploadFiles);
};

// 개별 파일 삭제
const handleRemove = (file: UploadUserFile) => {
  uploadRef.value?.handleRemove(file); // Element Plus의 내부 로직을 통해 파일 제거
};

// 전체 목록 비우기
const handleClearAll = () => {
  ElMessageBox.confirm('모든 파일을 목록에서 지우시겠습니까?', '경고', {
    confirmButtonText: '확인',
    cancelButtonText: '취소',
    type: 'warning',
  }).then(() => {
    uploadRef.value?.clearFiles();
    ElMessage.info('모든 파일이 목록에서 제거되었습니다.');
  }).catch(() => {});
};

const handleExceed: UploadProps['onExceed'] = (files) => {
  ElMessage.warning(`최대 ${props.limit}개의 파일만 추가할 수 있습니다.`);
};

const beforeUpload: UploadProps['beforeUpload'] = (rawFile: UploadRawFile) => {
  const fileExtension = rawFile.name.substring(rawFile.name.lastIndexOf('.') + 1).toLowerCase();
  if (!props.allowedExtensions.includes(fileExtension)) {
    ElMessage.error(`'${fileExtension}' 확장자는 허용되지 않습니다.`);
    return false;
  }
  if (rawFile.size / 1024 / 1024 > props.maxSizeMB) {
    ElMessage.error(`파일 크기는 ${props.maxSizeMB}MB를 초과할 수 없습니다!`);
    return false;
  }
  return true;
};

const handleSuccess: UploadProps['onSuccess'] = (response, uploadFile, uploadFiles) => {
  // 성공 이벤트는 개별 파일마다 발생하므로, 전체 목록을 다시 동기화 해주는 것이 좋음
  fileList.value = uploadFiles;
  emit('upload-success', { response, file: uploadFile, fileList: uploadFiles });
};

const handleError: UploadProps['onError'] = (error, uploadFile, uploadFiles) => {
  let errorMessage = '파일 업로드 실패';
  try {
    const errorResponse = JSON.parse(error.message);
    if (errorResponse?.message) errorMessage = errorResponse.message;
  } catch (e) {}
  ElMessage.error(`${uploadFile.name}: ${errorMessage}`);
  emit('upload-error', { error, file: uploadFile, fileList: uploadFiles });
};


// --- Expose (부모 컴포넌트에서 호출할 함수들) ---
const submit = () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('업로드할 파일이 없습니다.');
    return;
  }
  // 'success' 상태가 아닌 파일만 업로드 대상
  const filesToUpload = fileList.value.filter(file => file.status !== 'success');
  if (filesToUpload.length === 0) {
    ElMessage.info('모든 파일이 이미 업로드되었습니다.');
    return;
  }
  uploadRef.value?.submit();
};

const clearFiles = () => {
  uploadRef.value?.clearFiles();
};

defineExpose({
  submit,
  clearFiles,
  getFiles: () => fileList.value, // 현재 파일 목록을 가져오는 함수
});
</script>

<style scoped>
/* 최상위 컨테이너에 고정 높이 및 flexbox 설정 */
.compact-file-upload-container {
  width: calc(100% - 22px);
  height: 240px; /* 원하는 고정 높이로 설정 */
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  padding: 10px;
  display: flex;
  flex-direction: column; /* 자식 요소들을 수직으로 배치 */
  transition: border-color 0.3s;
}
.compact-file-upload-container:hover {
  border-color: var(--el-color-primary);
}

/* 파일 목록 래퍼가 남은 공간을 모두 차지하도록 설정 */
.file-list-wrapper {
  flex-grow: 1; /* 남은 수직 공간을 모두 차지 */
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 자식 요소가 넘칠 때를 대비 */
}

/* 실제 파일 목록(ul)이 스크롤되도록 설정 */
.compact-file-list {
  flex-grow: 1; /* 래퍼 안에서 남은 공간을 모두 차지 */
  overflow-y: auto; /* 내용이 넘치면 수직 스크롤바 생성 */
  list-style: none;
  padding: 0;
  margin: 0;
}

.file-list-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 0 5px 5px 5px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 5px;
  flex-shrink: 0; /* 헤더는 줄어들지 않음 */
}
.header-title { font-size: 14px; font-weight: 500; }
.header-info { font-size: 12px; color: var(--el-text-color-secondary); }

/* 파일 없을 때 플레이스홀더 */
.placeholder {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  text-align: center;
  color: var(--el-text-color-placeholder);
  font-size: 14px;
}
.placeholder p { margin: 0; }
.placeholder-rules { font-size: 12px; margin-top: 4px !important; color: var(--el-text-color-disabled); }

/* 개별 파일 아이템 */
.compact-file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 8px; /* 높이를 살짝 줄임 */
  border-radius: 4px;
  transition: background-color 0.2s ease;
}
.compact-file-item:hover { background-color: var(--el-fill-color-light); }
.file-info { display: flex; align-items: center; gap: 8px; flex-grow: 1; overflow: hidden; }
.file-info .el-icon { color: var(--el-text-color-regular); flex-shrink: 0; }
.file-name { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-size: 14px; }
.file-size { font-size: 12px; color: var(--el-text-color-secondary); flex-shrink: 0; margin-left: 8px; }

.file-status-actions { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.file-status { width: 20px; height: 20px; display: flex; align-items: center; justify-content: center; }
.status-label { display: flex; align-items: center; font-size: 18px; }
.status-label.ready { color: var(--el-color-info-light-3); }
.status-label.success { color: var(--el-color-success); }
.status-label.fail { color: var(--el-color-error); }

/* 업로드 버튼 영역은 크기가 변하지 않도록 설정 */
.upload-trigger-area {
  flex-shrink: 0; /* 버튼 영역은 줄어들지 않음 */
  padding-top: 10px;
}
.upload-actions { display: flex; gap: 10px; }

/* 파일 목록 애니메이션 */
.file-list-anim-enter-active, .file-list-anim-leave-active { transition: all 0.3s ease; }
.file-list-anim-enter-from, .file-list-anim-leave-to { opacity: 0; transform: translateY(-10px); }
</style>