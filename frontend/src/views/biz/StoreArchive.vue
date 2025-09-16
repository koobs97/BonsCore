<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { Plus, Edit, Delete, Search, StarFilled, MoreFilled, Sunny, Moon } from '@element-plus/icons-vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';

import StoreFormDialog from '@/components/biz/StoreFormDialog.vue'

// --- 상태 관리 ---
const stores = ref<any[]>([]);
const isLoading = ref(true);
const dialogVisible = ref(false);
const isEditMode = ref(false);
const formRef = ref<FormInstance>();
const searchQuery = ref('');
const activeStoreId = ref<number | null>(null); // ★ 새로 추가: 현재 선택된 맛집 ID

const dialogState = ref({
  visible: false,
  isEditMode: false,
  initialData: {},
});

// --- 폼 데이터 모델 ---
const initialFormData = {
  id: null,
  name: '',
  category: '',
  rating: 0,
  visitDate: '',
  memo: '',
};
const formData = ref({ ...initialFormData });

// --- 폼 유효성 검사 규칙 ---
const rules: FormRules = {
  name: [{ required: true, message: '가게 이름을 입력해주세요.', trigger: 'blur' }],
  category: [{ required: true, message: '카테고리를 입력해주세요.', trigger: 'blur' }],
  visitDate: [{ required: true, message: '방문 날짜를 선택해주세요.', trigger: 'change' }],
  rating: [{ required: true, message: '별점을 선택해주세요.', trigger: 'change', type: 'number', min: 1, max: 5 }],
};

// --- Mock 데이터 (기존과 동일) ---
const fetchStores = () => {
  isLoading.value = true;
  setTimeout(() => {
    stores.value = [
      { id: 1, name: '런던 베이글 뮤지엄', category: '베이커리', rating: 5, visitDate: '2023-10-26', memo: '소금빵이랑 쪽파 베이글이 인상적이었음.\n웨이팅이 길지만 가볼만 하다.' },
      { id: 2, name: '카멜 커피', category: '카페', rating: 4, visitDate: '2023-09-15', memo: '앙버터와 카멜커피 조합이 좋았다.\n매장이 협소해서 테이크아웃 추천.' },
      { id: 3, name: '다운타우너 안국', category: '수제버거', rating: 5, visitDate: '2023-08-01', memo: '아보카도 버거는 진리!\n감자튀김도 바삭하고 맛있음.' },
      { id: 4, name: '고든램지 버거', category: '수제버거', rating: 3, visitDate: '2023-11-05', memo: '가격에 비해 특별한 맛은 아니었음. 헬스키친 버거는 괜찮았다.' },
      { id: 5, name: '진작', category: '일식', rating: 4, visitDate: '2023-07-22', memo: '후토마키랑 대창덮밥이 유명함. 재료가 신선하고 양도 많다.' },
    ].sort((a, b) => new Date(b.visitDate).getTime() - new Date(a.visitDate).getTime()); // 최신 방문일 순으로 정렬
    isLoading.value = false;
    // ★ 새로 추가: 로딩 후 첫 번째 맛집을 자동으로 선택
    if (stores.value.length > 0) {
      activeStoreId.value = stores.value[0].id;
    }
  }, 1000);
};

onMounted(() => {
  fetchStores();
});

// --- 검색 기능 (기존과 동일) ---
const filteredStores = computed(() => {
  if (!searchQuery.value) {
    return stores.value;
  }
  return stores.value.filter(store =>
      store.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      store.category.toLowerCase().includes(searchQuery.value.toLowerCase())
  );
});

// ★ 새로 추가: 선택된 맛집의 상세 정보를 가져오는 computed 속성
const selectedStore = computed(() => {
  if (!activeStoreId.value) return null;
  return stores.value.find(s => s.id === activeStoreId.value);
});

// 검색 결과가 변경될 때, 선택된 항목이 사라졌다면 선택을 초기화
watch(filteredStores, (newVal) => {
  if (activeStoreId.value && !newVal.some(s => s.id === activeStoreId.value)) {
    activeStoreId.value = newVal.length > 0 ? newVal[0].id : null;
  }
});


// --- 다이얼로그 제어
const openAddDialog = () => {
  dialogState.value = {
    visible: true,
    isEditMode: false,
    initialData: { // 빈 폼의 기본값 설정
      id: null,
      name: '',
      category: '',
      rating: 0,
      visitDate: new Date().toISOString().split('T')[0],
      memo: '',
    },
  };
};

const openEditDialog = (store: any) => {
  dialogState.value = {
    visible: true,
    isEditMode: true,
    initialData: { ...store }, // 수정할 데이터 전달
  };
};

const handleFormSubmit = (formData: any) => {
  if (dialogState.value.isEditMode) {
    const index = stores.value.findIndex(s => s.id === formData.id);
    if (index !== -1) stores.value[index] = { ...formData };
    ElMessage.success('정보가 수정되었습니다.');
  } else {
    const newStore = { ...formData, id: Date.now() };
    stores.value.unshift(newStore);
    activeStoreId.value = newStore.id;
    ElMessage.success('새로운 맛집이 추가되었습니다.');
  }
  dialogState.value.visible = false; // 다이얼로그 닫기
};

const closeDialog = () => {
  dialogVisible.value = false;
};

// --- CRUD 기능 ---
const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate((valid) => {
    if (valid) {
      if (isEditMode.value) {
        const index = stores.value.findIndex(s => s.id === formData.value.id);
        if (index !== -1) stores.value[index] = { ...formData.value };
        ElMessage.success('정보가 수정되었습니다.');
      } else {
        const newStore = { ...formData.value, id: Date.now() };
        stores.value.unshift(newStore);
        activeStoreId.value = newStore.id; // 새로 추가한 항목을 바로 선택
        ElMessage.success('새로운 맛집이 추가되었습니다.');
      }
      closeDialog();
    }
  });
};

const handleDelete = (storeId: number) => {
  ElMessageBox.confirm('정말로 삭제하시겠습니까?', '삭제 확인', {
    confirmButtonText: '삭제',
    cancelButtonText: '취소',
    type: 'warning',
  }).then(() => {
    const deletedIndex = stores.value.findIndex(s => s.id === storeId);
    stores.value = stores.value.filter(s => s.id !== storeId);

    // 삭제 후 선택 항목 조정
    if (stores.value.length > 0) {
      const newIndex = Math.max(0, deletedIndex -1);
      activeStoreId.value = stores.value[newIndex].id;
    } else {
      activeStoreId.value = null;
    }
    ElMessage.success('삭제되었습니다.');
  }).catch(() => {});
};
</script>

<template>
  <div class="archive-container">
    <!-- 왼쪽 목록 패널 -->
    <div class="list-panel">
      <div class="list-header">
        <h2 class="list-title">저장소</h2>
        <el-button type="primary" :icon="Plus" circle @click="openAddDialog" />
      </div>
      <div class="search-wrapper">
        <el-input v-model="searchQuery" placeholder="맛집 검색..." :prefix-icon="Search" clearable />
      </div>
      <div class="store-list">
        <el-skeleton :rows="5" animated v-if="isLoading" />
        <template v-else>
          <div
              v-for="store in filteredStores"
              :key="store.id"
              class="list-item"
              :class="{ active: activeStoreId === store.id }"
              @click="activeStoreId = store.id"
          >
            <div class="item-info">
              <span class="item-name">{{ store.name }}</span>
              <span class="item-category">{{ store.category }}</span>
            </div>
            <span class="item-date">{{ store.visitDate }}</span>
          </div>
          <el-empty v-if="filteredStores.length === 0" description="결과가 없습니다" :image-size="80" />
        </template>
      </div>
    </div>

    <!-- 오른쪽 상세 정보 패널 -->
    <div class="detail-panel">
      <div v-if="selectedStore" class="detail-content">
        <div class="detail-header">
          <div class="header-text">
            <h1>{{ selectedStore.name }}</h1>
            <el-tag type="info" round>{{ selectedStore.category }}</el-tag>
          </div>
          <div class="header-actions">
            <el-button :icon="Edit" text circle @click="openEditDialog(selectedStore)" />
            <el-button type="danger" :icon="Delete" text circle @click="handleDelete(selectedStore.id)" />
          </div>
        </div>
        <div class="detail-body">
          <div class="info-group">
            <div class="info-item">
              <span class="label">방문일</span>
              <span class="value">{{ selectedStore.visitDate }}</span>
            </div>
            <div class="info-item">
              <span class="label">별점</span>
              <el-rate :model-value="selectedStore.rating" disabled size="large" />
            </div>
          </div>
          <div class="memo-section">
            <h3 class="memo-title">Memo</h3>
            <p class="memo-text">{{ selectedStore.memo }}</p>
          </div>
        </div>
      </div>
      <el-empty v-else description="맛집을 선택해주세요" class="empty-detail" />
    </div>

    <StoreFormDialog
        v-model:visible="dialogState.visible"
        :is-edit-mode="dialogState.isEditMode"
        :initial-data="dialogState.initialData"
        @submit="handleFormSubmit"
    />
  </div>
</template>

<style scoped>

.list-title, .detail-header h1 {
  font-family: 'Poppins', 'Noto Sans KR', sans-serif;
}

/* 전체 컨테이너 */
.archive-container {
  display: flex;
  margin-top: 4px;
  width: calc(100% - 2px);
  height: calc(100% - 52px);
  /* ★ 수정: body가 아닌 컴포넌트 배경색 변수 사용 */
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  overflow: hidden;
  /* ★ 추가: 색상 전환 시 부드러운 효과 */
  transition: background-color 0.3s, border-color 0.3s;
}

/* 왼쪽 목록 패널 */
.list-panel {
  width: 300px;
  /* ★ 수정: 테마에 맞는 연한 배경색 사용 */
  background-color: var(--el-fill-color-lighter);
  border-right: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: background-color 0.3s, border-color 0.3s;
}
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid var(--el-border-color-light);
  flex-shrink: 0;
  transition: border-color 0.3s;
}
.list-title {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 700;
  /* ★ 수정: 기본 텍스트 색상 변수 사용 */
  color: var(--el-text-color-primary);
}
.search-wrapper {
  padding: 12px 8px 12px 8px;
  flex-shrink: 0;
}
.store-list {
  flex-grow: 1;
  overflow-y: auto;
  padding: 0 8px 8px 8px;
}
.store-list::-webkit-scrollbar { width: 4px; }
.store-list::-webkit-scrollbar-thumb { background-color: var(--el-border-color-lighter); border-radius: 2px; }

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
  margin-bottom: 6px;
}
.list-item:hover {
  /* ★ 수정: 테마의 primary-light-9 색상 사용 */
  background-color: var(--el-color-primary-light-9);
}
/* ★★★★★ 여기가 핵심 수정 지점 ★★★★★ */
.list-item.active {
  /* ★ 수정: 활성 배경은 primary 색상 */
  background-color: var(--el-color-primary);
  /* ★ 수정: 활성 글자색은 배경색과 대비되는 색 (라이트: 흰색, 다크: 검정) */
  color: var(--el-bg-color);
}

.item-info { display: flex; flex-direction: column; gap: 2px; }
.item-name {
  font-weight: 600;
  font-size: 0.95rem;
  /* ★ 수정: 기본 텍스트 색상, 활성 상태에선 상속받음 */
  color: var(--el-text-color-primary);
}
.item-category, .item-date {
  font-size: 0.8rem;
  /* ★ 수정: 보조 텍스트 색상, 활성 상태에선 상속받음 */
  color: var(--el-text-color-secondary);
  text-align: left;
}

.list-item:hover .item-name { color: var(--el-text-color-primary); }
.list-item:hover .item-category, .list-item:hover .item-date { color: var(--el-text-color-secondary); }

/* 활성 상태일 때 내부 텍스트 색상 강제 지정 */
.list-item.active .item-name,
.list-item.active .item-category,
.list-item.active .item-date {
  color: var(--el-bg-color); /* 배경색과 대비되는 색상으로 덮어쓰기 */
  opacity: 0.9;
}
.list-item.active .item-name {
  opacity: 1;
}

/* 오른쪽 상세 정보 패널 */
.detail-panel {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  padding: 30px 40px;
  overflow-y: auto;
}
.detail-content { width: 100%; }
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--el-border-color-light);
  transition: border-color 0.3s;
}
.detail-header h1 {
  margin: 0 0 8px 0;
  font-size: 2rem;
  font-weight: 800;
  color: var(--el-text-color-primary);
}

.header-text {
  text-align: left;
}

.header-actions .el-button { font-size: 18px; outline: none; }

.detail-body { margin-top: 24px; }
.info-group {
  display: flex;
  gap: 40px;
  /* ★ 수정: 테마에 맞는 연한 배경색 사용 */
  background-color: var(--el-fill-color-light);
  padding: 20px;
  border-radius: 8px;
  transition: background-color 0.3s;
}
.info-item { display: flex; flex-direction: column; gap: 8px; }
.info-item .label { font-size: 0.85rem; color: var(--el-text-color-secondary); }
.info-item .value { font-size: 1rem; font-weight: 500; color: var(--el-text-color-primary); }
:deep(.el-rate) { height: auto; }

.memo-section { margin-top: 30px; }
.memo-title {
  margin: 0 0 12px 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}
.memo-text {
  font-size: 0.95rem;
  line-height: 1.7;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  /* ★ 수정: 테마에 맞는 연한 배경색 사용 */
  background-color: var(--el-fill-color-lighter);
  padding: 16px;
  border-radius: 6px;
  min-height: 150px;
  transition: background-color 0.3s, color 0.3s;
}

.empty-detail {
  margin: auto;
}

/* 다크 모드에서 별점 비활성 색상 조정 */
html.dark .info-group :deep(.el-rate__void) {
  color: var(--el-fill-color-darker) !important;
}
</style>