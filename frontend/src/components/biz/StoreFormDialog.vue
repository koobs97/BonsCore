<!-- src/components/StoreFormDialog.vue -->
<script setup lang="ts">
import { ref, watch, nextTick } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { Shop, CollectionTag, Calendar, EditPen, StarFilled, Check } from '@element-plus/icons-vue';

// --- Props & Emits (기존과 동일) ---
const props = defineProps({
  visible: { type: Boolean, required: true },
  isEditMode: { type: Boolean, default: false },
  initialData: {
    type: Object,
    default: () => ({ id: null, name: '', category: '', rating: 0, visitDate: new Date().toISOString().split('T')[0], memo: '' }),
  },
});
const emit = defineEmits(['update:visible', 'submit']);

// --- 내부 상태 및 로직 (기존과 동일) ---
const formRef = ref<FormInstance>();
const formData = ref({ ...props.initialData });

watch(() => props.initialData, (newData) => {
  formData.value = { ...newData };
  nextTick(() => { formRef.value?.clearValidate(); });
}, { deep: true });

const rules: FormRules = {
  name: [{ required: true, message: '가게 이름을 입력해주세요.', trigger: 'blur' }],
  category: [{ required: true, message: '카테고리를 입력해주세요.', trigger: 'blur' }],
  visitDate: [{ required: true, message: '방문 날짜를 선택해주세요.', trigger: 'change' }],
  rating: [{ required: true, message: '별점을 선택해주세요.', trigger: 'change', type: 'number', min: 1 }],
};

const handleClose = () => { emit('update:visible', false); };
const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate((valid) => { if (valid) { emit('submit', formData.value); } });
};
</script>

<template>
  <!-- Vue의 Transition 컴포넌트로 부드러운 등장/사라짐 효과 추가 -->
  <Transition name="dialog-fade">
    <el-dialog
        v-if="visible"
        :model-value="visible"
        :show-close="false"
        width="640px"
        class="store-form-dialog"
        top="15vh"
        @close="handleClose"
        append-to-body
        :close-on-click-modal="false"
    >
      <template #header="{ close }">
        <div class="dialog-header-custom">
          <div class="header-main-content">
            <div class="header-icon">
              <el-icon v-if="isEditMode" :size="24"><EditPen /></el-icon>
              <el-icon v-else :size="24"><Shop /></el-icon>
            </div>
            <div class="header-title-group">
              <h2 class="header-title">{{ isEditMode ? '맛집 기록 수정' : '새로운 맛집 기록' }}</h2>
              <p class="header-subtitle">소중한 미식 경험을 기록해보세요.</p>
            </div>
          </div>
          <el-button text circle class="close-btn" @click="close">✕</el-button>
        </div>
      </template>

      <div class="dialog-content">
        <div class="dialog-illustration">
          <svg class="animated-svg-cutlery" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
            <!-- 접시 -->
            <circle cx="50" cy="50" r="40" stroke="var(--el-border-color-light)" stroke-width="2.5" fill="none"/>
            <circle cx="50" cy="50" r="30" stroke="var(--el-border-color-lighter)" stroke-width="2" fill="none"/>
            <!-- 포크 -->
            <g class="fork">
              <path d="M35 70 V 35 Q 35 20 40 20 H 40 Q 45 20 45 35 V 70" stroke="var(--el-text-color-placeholder)" stroke-width="2.5" fill="none" stroke-linecap="round"/>
              <path d="M35 35 L 28 20 M40 35 L 40 20 M45 35 L 52 20" stroke="var(--el-text-color-placeholder)" stroke-width="2.5" fill="none" stroke-linecap="round"/>
            </g>
            <!-- 나이프 -->
            <g class="knife">
              <path d="M65 70 V 35 Q 65 20 60 20 H 60 Q 55 20 55 35 V 70" stroke="var(--el-text-color-placeholder)" stroke-width="2.5" fill="none" stroke-linecap="round"/>
            </g>
          </svg>
          <p class="illustration-text">"맛있으면 0칼로리"</p>
        </div>
        <el-form ref="formRef" :model="formData" :rules="rules" label-position="top" class="store-form">
          <el-form-item prop="name">
            <template #label><span class="form-label-with-icon"><el-icon><Shop /></el-icon> 가게 이름</span></template>
            <el-input
                v-model="formData.name"
                placeholder="기억에 남는 그곳의 이름은?"
                size="large"
                autofocus
                maxlength="25"
                show-word-limit
            />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item prop="category">
                <template #label><span class="form-label-with-icon"><el-icon><CollectionTag /></el-icon> 카테고리</span></template>
                <el-input v-model="formData.category" placeholder="예: 한식, 카페" size="large"/>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item prop="visitDate">
                <template #label><span class="form-label-with-icon"><el-icon><Calendar /></el-icon> 방문일</span></template>
                <el-date-picker v-model="formData.visitDate" type="date" placeholder="날짜 선택" format="YYYY-MM-DD" value-format="YYYY-MM-DD" size="large" style="width: 100%;"/>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item prop="rating" class="rating-form-item">
            <template #label><span class="form-label-with-icon">별점</span></template>
            <el-rate v-model="formData.rating" :max="5" class="custom-rate"/>
          </el-form-item>
          <el-form-item prop="memo">
            <template #label><span class="form-label-with-icon"><el-icon><EditPen /></el-icon> 한 줄 메모</span></template>
            <el-input
                v-model="formData.memo"
                type="textarea"
                :rows="3"
                placeholder="어떤 점이 특별했나요?"
                resize="none"
                maxlength="200"
                show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleClose" size="large" text>취소</el-button>
          <el-button type="primary" @click="handleSubmit" size="large" :icon="Check" round>
            {{ isEditMode ? '수정 완료' : '기록 저장' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </Transition>
</template>

<style scoped>
/* 모던 다이얼로그 스타일 */
.store-form-dialog :deep(.el-dialog) {
  border-radius: 20px;
  box-shadow: 0 25px 50px -12px rgb(0 0 0 / 0.25);
  background-color: var(--el-bg-color); /* 테마 배경색 적용 */
  overflow: visible; /* 헤더 일러스트가 밖으로 나갈 수 있도록 */
}

/* === 헤더 === */
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
  /* position, top, right 속성 제거됨 */
  margin: 0;
  font-size: 1.2rem;
  color: var(--el-text-color-placeholder);
  --el-button-hover-text-color: var(--el-color-primary);
  --el-button-hover-bg-color: var(--el-color-primary-light-9);
}

/* === 바디 === */
.store-form-dialog :deep(.el-dialog__body) { padding: 32px; }
.dialog-content { display: flex; align-items: flex-start; gap: 32px; }

/* 일러스트레이션 */
.dialog-illustration {
  position: relative;
  width: 150px;
  text-align: center;
  margin-top: 20px;
}
.animated-svg-cutlery {
  animation: gentle-rotate 10s linear infinite;
}
.animated-svg-cutlery .fork, .animated-svg-cutlery .knife {
  transform-origin: center;
  animation: sway 5s ease-in-out infinite alternate;
}
.animated-svg-cutlery .knife {
  animation-delay: -2.5s;
}

@keyframes gentle-rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
@keyframes sway {
  from { transform: rotate(-5deg); }
  to { transform: rotate(5deg); }
}
.illustration-text {
  font-family: 'Outfit', sans-serif;
  font-size: 0.8rem;
  color: var(--el-text-color-placeholder);
  margin-top: 16px;
}

/* 폼 스타일 */
.store-form { flex: 1; }
.form-label-with-icon {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
/* 입력창 스타일 개선 */
:deep(.el-input__wrapper), :deep(.el-textarea__inner) {
  background-color: var(--el-fill-color-lighter);
  box-shadow: none !important;
  border-radius: 8px;
}
:deep(.el-input__wrapper:hover), :deep(.el-textarea__inner:hover) {
  background-color: var(--el-fill-color-light);
}
:deep(.el-input__wrapper.is-focus), :deep(.el-textarea__inner:focus) {
  background-color: var(--el-bg-color);
  box-shadow: 0 0 0 1px var(--el-color-primary) inset !important;
}

/* 별점 */
.rating-form-item :deep(.el-form-item__content) { height: 40px; align-items: center; }
.custom-rate { --el-rate-font-size: 32px; }
.custom-rate :deep(.el-rate__icon) {
  margin-right: 6px;
  transition: transform 0.2s ease;
}
.custom-rate :deep(.el-rate__icon:hover) {
  transform: scale(1.2);
}

/* === 푸터 === */
.store-form-dialog :deep(.el-dialog__footer) {
  padding: 24px 32px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; }
.dialog-footer .el-button--primary {
  --el-button-hover-bg-color: var(--el-color-primary-dark-2);
}

/* === 다이얼로그 등장 애니메이션 === */
.dialog-fade-enter-active {
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.dialog-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(20px);
}
</style>