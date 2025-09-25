<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
// FIXED: 템플릿에서 사용하는 모든 아이콘을 import 합니다.
import { Plus, Edit, Delete, Search, Link, Calendar, StarFilled, Picture } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import StoreFormDialog from '@/components/biz/StoreFormDialog.vue';
import { Api } from '@/api/axiosInstance';
import { ApiUrls } from '@/api/apiUrls';
import { userStore } from "@/store/userStore";

// --- 상태 (State) ---
const userStoreObj = userStore();
const stores = ref<any[]>([]);
const isLoading = ref(true);
const searchQuery = ref('');
const activeStoreId = ref<number | null>(null);
const dialogState = ref({
  visible: false,
  isEditMode: false,
  initialData: {},
});

// --- ★블로그 스타일을 위한 새로운 상태 추가★ ---
/** 현재 메인에 보여줄 이미지의 인덱스 */
const currentImageIndex = ref(0);

// --- 기존 함수 (수정 없음) ---
const formatDateToMonthDay = (date: any): string => {
  if (!date) return '';
  try {
    const d = new Date(date);
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${month}-${day}`;
  } catch (e) {
    return String(date);
  }
};

const fetchStores = async () => {
  isLoading.value = true;
  try {
    const payload = { userId: userStoreObj.getUserInfo.userId };
    const response = await Api.post(ApiUrls.GET_GOURMET_RECORDS, payload);
    stores.value = response.data;

    if (stores.value.length > 0) {
      // 현재 선택된 ID가 없거나, 목록에 더 이상 존재하지 않으면 첫 번째 항목을 선택
      if (!activeStoreId.value || !stores.value.some(s => s.id === activeStoreId.value)) {
        activeStoreId.value = stores.value[0].id;
      }
    } else {
      activeStoreId.value = null;
    }
  } catch (error) { } finally {
    isLoading.value = false;
  }
};

const handleDelete = (storeId: number) => {
  ElMessageBox.confirm('이 기록을 정말로 삭제하시겠습니까?', '삭제 확인', {
    confirmButtonText: '삭제', cancelButtonText: '취소', type: 'warning',
  }).then(async () => {
    try {
      ElMessage.success('기록이 삭제되었습니다.');
      await fetchStores();
    } catch (error) {
      console.error("삭제 처리 실패:", error);
      ElMessage.error('삭제 중 오류가 발생했습니다.');
    }
  }).catch(() => {});
};

onMounted(fetchStores);

const filteredStores = computed(() => {
  if (!searchQuery.value) return stores.value;
  return stores.value.filter(store =>
      store.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      store.category.toLowerCase().includes(searchQuery.value.toLowerCase())
  );
});

const selectedStore = computed(() => {
  if (!activeStoreId.value) return null;
  return stores.value.find(s => s.id === activeStoreId.value);
});

// --- ★블로그 스타일을 위한 computed 속성 및 함수 추가★ ---

// 이미지 확대보기 기능에 사용할 전체 이미지 URL 배열
const selectedStoreImageUrls = computed(() => {
  return selectedStore.value?.images?.map((i: any) => i.imageUrl) || [];
});

// 현재 메인에 표시할 이미지 URL
const currentMainImageUrl = computed(() => {
  if (selectedStore.value?.images?.length > 0) {
    return selectedStore.value.images[currentImageIndex.value]?.imageUrl || '';
  }
  return '';
});

// 썸네일 클릭 시 메인 이미지를 변경하는 함수
const selectImage = (index: number) => {
  currentImageIndex.value = index;
};

// --- Watchers 및 Dialog 함수 (수정 없음) ---
watch(selectedStore, () => {
  // 가게 선택이 바뀌면, 이미지 인덱스를 첫 번째로 초기화
  currentImageIndex.value = 0;
});

watch(filteredStores, (newVal) => {
  if (activeStoreId.value && !newVal.some(s => s.id === activeStoreId.value)) {
    activeStoreId.value = newVal.length > 0 ? newVal[0].id : null;
  }
});

const openAddDialog = () => {
  dialogState.value = {
    visible: true, isEditMode: false,
    initialData: {
      id: null, name: '', category: '', rating: 0,
      visitDate: new Date().toISOString().split('T')[0],
      memo: '', referenceUrl: '', images: [],
    },
  };
};

const openEditDialog = (store: any) => {
  dialogState.value = { visible: true, isEditMode: true, initialData: { ...store } };
};

const handleFormSubmit = () => {
  fetchStores();
  dialogState.value.visible = false;
};

const prevImage = () => {
  if (!selectedStore.value || selectedStore.value.images.length <= 1) return;
  const imageCount = selectedStore.value.images.length;
  // 현재 인덱스에서 1을 빼고, 음수가 되면 마지막 인덱스로 순환
  currentImageIndex.value = (currentImageIndex.value - 1 + imageCount) % imageCount;
};

const nextImage = () => {
  if (!selectedStore.value || selectedStore.value.images.length <= 1) return;
  const imageCount = selectedStore.value.images.length;
  // 현재 인덱스에서 1을 더하고, 총 개수를 넘어가면 0으로 순환
  currentImageIndex.value = (currentImageIndex.value + 1) % imageCount;
};

</script>

<template>
  <!-- <template> 내용은 이전과 동일합니다. 변경 사항 없습니다. -->
  <div class="archive-container">
    <div class="list-panel">
      <div class="list-header">
        <h2 class="list-title">저장소</h2>
        <el-button type="primary" :icon="Plus" circle @click="openAddDialog" />
      </div>
      <div class="search-wrapper">
        <el-input v-model="searchQuery" placeholder="이름 또는 카테고리 검색" :prefix-icon="Search" clearable />
      </div>
      <div class="store-list">
        <el-skeleton :rows="6" animated v-if="isLoading" />
        <template v-else>
          <div v-for="store in filteredStores" :key="store.id" class="list-item" :class="{ active: activeStoreId === store.id }" @click="activeStoreId = store.id">
            <div class="item-info">
              <span class="item-name">{{ store.name }}</span>
              <span class="item-category">{{ store.category }}</span>
            </div>
            <span class="item-date">{{ formatDateToMonthDay(store.visitDate) }}</span>
          </div>
          <el-empty v-if="!filteredStores.length" description="결과가 없습니다" :image-size="60" />
        </template>
      </div>
    </div>

    <div class="detail-panel">
      <div v-if="selectedStore" class="detail-wrapper">
        <header class="detail-header">
          <el-tag type="info" round effect="plain" size="small">{{ selectedStore.category }}</el-tag>
          <div class="title-group">

            <h1 class="store-name">{{ selectedStore.name }}</h1>
          </div>
          <div class="actions-group">
            <el-button :icon="Edit" text circle @click="openEditDialog(selectedStore)" title="수정" />
            <el-button type="danger" :icon="Delete" text circle @click="handleDelete(selectedStore.id)" title="삭제" />
          </div>
        </header>

        <main class="detail-body">
          <section class="gallery-section">
            <template v-if="selectedStore.images && selectedStore.images.length > 0">
              <div class="gallery-section">
                <div class="main-image-container">
                  <!-- ★★★ [수정] 부드러운 전환을 위해 transition 태그로 감싸고 key 추가 ★★★ -->
                  <transition name="fade" mode="out-in">
                    <el-image
                        :key="currentMainImageUrl"
                        :src="currentMainImageUrl"
                        fit="cover"
                        class="main-image"
                        :preview-src-list="selectedStoreImageUrls"
                        :initial-index="currentImageIndex"
                        hide-on-click-modal
                    />
                  </transition>

                  <div class="image-count-badge">
                    {{ currentImageIndex + 1 }} / {{ selectedStore.images.length }}
                  </div>

                  <!-- 이전/다음 화살표 버튼 (기존과 동일) -->
                  <div v-if="selectedStore.images.length > 1">
                    <div class="arrow left" @click="prevImage">
                      <el-icon><ArrowLeftBold /></el-icon>
                    </div>
                    <div class="arrow right" @click="nextImage">
                      <el-icon><ArrowRightBold /></el-icon>
                    </div>
                  </div>
                </div>

                <div class="thumbnail-list-wrapper" v-if="selectedStore.images.length > 1">
                  <div class="thumbnail-list" ref="thumbnailList">
                    <div
                        v-for="(image, index) in selectedStore.images"
                        :key="index"
                        class="thumbnail"
                        :class="{ active: index === currentImageIndex }"
                        @click="selectImage(index)"
                    >
                      <el-image :src="image.imageUrl" fit="cover" class="thumbnail-image" />
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="no-image-placeholder">
                <el-icon><Picture /></el-icon>
                <span>No Photos</span>
              </div>
            </template>
          </section>

          <section class="details-section">
<!--            <h3 class="section-title">Details</h3>-->
            <div class="details-grid">
              <div class="detail-item">

                <div class="item-text">
                  <div class="label-with-icon">
                    <span class="label">방문일</span>
                    <el-icon class="item-icon"><Calendar /></el-icon>
                  </div>
                  <span class="value">{{ new Date(selectedStore.visitDate).toLocaleDateString('ko-KR') }}</span>
                </div>
              </div>
              <div class="detail-item">
                <div class="item-text">
                  <div class="label-with-icon">
                    <span class="label">별점</span>
                    <el-icon class="item-icon"><StarFilled /></el-icon>
                  </div>
                  <div>
                    <el-rate :model-value="selectedStore.rating" disabled size="small" />
                  </div>
                </div>
              </div>
              <div class="detail-item" v-if="selectedStore.referenceUrl">

                <div class="item-text">
                  <div class="label-with-icon">
                    <span class="label">참조 링크</span>
                    <el-icon class="item-icon"><Link /></el-icon>
                  </div>
                  <a :href="selectedStore.referenceUrl" target="_blank" class="value-link">바로가기</a>
                </div>
              </div>
            </div>
          </section>

          <section class="memo-section">
<!--            <h3 class="section-title">Memo</h3>-->
            <p class="memo-text">{{ selectedStore.memo || '작성된 메모가 없습니다.' }}</p>
          </section>
        </main>
      </div>
      <el-empty v-else-if="!isLoading" description="기록을 선택해주세요." class="empty-detail" />
    </div>

    <StoreFormDialog v-model:visible="dialogState.visible" :is-edit-mode="dialogState.isEditMode" :initial-data="dialogState.initialData" @submit="handleFormSubmit" />
  </div>
</template>

<style scoped>
/* --- 기본 컨테이너 및 리스트 패널 (변경 없음) --- */
.archive-container {
  width: 820px;
  height: 620px;
  display: flex;
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  margin: 4px 0 0 0;
  overflow: hidden;
  font-family: 'Noto Sans KR', sans-serif;
  box-shadow: 0 4px 12px rgba(0,0,0, 0.05);
}
.list-panel {
  width: 300px; background-color: var(--el-bg-color-page);
  border-right: 1px solid var(--el-border-color-lighter);
  display: flex; flex-direction: column; flex-shrink: 0;
}
.list-header {
  padding: 16px; border-bottom: 1px solid var(--el-border-color-lighter);
  display: flex; justify-content: space-between; align-items: center; flex-shrink: 0;
}
.list-title {
  font-size: 1.2rem;
  font-weight: 600;
  margin: 0;
  font-family: 'Poppins', sans-serif;
  color: var(--el-color-primary);
}
.search-wrapper { padding: 8px 8px 16px 8px; flex-shrink: 0; }
.store-list { flex-grow: 1; overflow-y: auto; padding: 0 8px 8px; }
.store-list::-webkit-scrollbar { width: 4px; }
.store-list::-webkit-scrollbar-thumb { background-color: var(--el-border-color-light); border-radius: 2px; }

.list-item {
  padding: 12px;
  margin-bottom: 4px;
  border-radius: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.2s ease;
  border: 1px solid var(--el-border-color-light);
}
.list-item:hover { background-color: var(--el-fill-color-light); }
.list-item.active {
  background-color: var(--el-color-primary);
  color: #fff;
  box-shadow: 0 2px 8px rgba(var(--el-color-primary-rgb), 0.3);
}
.item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
  text-align: left;
}
.item-name {
  font-weight: 500;
  font-size: 0.95rem;
  color: var(--el-text-color-placeholder);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-category {
  font-size: 0.8rem;
  color: var(--el-text-color-placeholder);
  margin-left: 1px;
}
.item-date { font-size: 0.8rem; color: var(--el-text-color-placeholder); flex-shrink: 0; margin-left: 8px; }
.list-item.active .item-name, .list-item.active .item-category, .list-item.active .item-date { color: var(--el-bg-color); }

.detail-panel {
  flex: 1; display: flex; flex-direction: column;
  overflow-y: auto; background-color: var(--el-bg-color);
}
.detail-panel::-webkit-scrollbar { width: 5px; }
.detail-panel::-webkit-scrollbar-thumb { background-color: var(--el-border-color); border-radius: 3px; }

.detail-wrapper {
  padding: 0 32px 16px 32px;
}

.detail-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 18px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 12px;
}
.title-group {
  display: flex;
  gap: 8px;
}
.store-name {
  font-size: 1.8rem;
  font-weight: 700;
  line-height: 1.2;
  margin: 0;
  color: var(--el-text-color-primary);
  font-family: 'Poppins', sans-serif;
}
.actions-group .el-button { font-size: 18px; color: var(--el-text-color-secondary); }

.detail-body section {
  margin-bottom: 12px;
}
.detail-body section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 1rem; font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 16px 0;
}

.gallery-section .main-image-container {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 12px;
  overflow: hidden;
  background-color: var(--el-fill-color-light);
  margin-bottom: 12px;
}

.main-image {
  width: 100%;
  height: 100%;
}

.image-count-badge {
  position: absolute;
  bottom: 12px;
  right: 12px;
  padding: 4px 8px;
  border-radius: 12px;
  background-color: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 0.75rem;
  font-weight: 500;
  z-index: 1;
}

/* 화살표 스타일 */
.main-image-container .arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  background-color: rgba(0, 0, 0, 0.4);
  color: white;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.3s;
  z-index: 2;
  font-size: 1.2rem;
}

.main-image-container .arrow:hover {
  background-color: rgba(0, 0, 0, 0.6);
}

.main-image-container .arrow.left {
  left: 12px;
}

.main-image-container .arrow.right {
  right: 12px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.thumbnail-list-wrapper {
  overflow-x: auto;
  padding-bottom: 8px; /* 스크롤바 공간 확보 */
}

/* 스크롤바 스타일링 (선택 사항) */
.thumbnail-list-wrapper::-webkit-scrollbar {
  height: 6px;
}
.thumbnail-list-wrapper::-webkit-scrollbar-thumb {
  background-color: #ccc;
  border-radius: 10px;
}
.thumbnail-list-wrapper::-webkit-scrollbar-track {
  background-color: #f1f1f1;
}


.thumbnail-list {
  display: flex;
  gap: 10px;
  padding-bottom: 4px;
}

.thumbnail {
  width: 60px; /* 썸네일 크기 약간 축소 */
  height: 60px;
  border-radius: 8px;
  border: 2px solid transparent;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.thumbnail:hover {
  border-color: var(--el-color-primary-light-3);
  transform: translateY(-2px);
}

.thumbnail.active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 8px rgba(var(--el-color-primary-rgb), 0.4);
}

.thumbnail-image {
  width: 100%;
  height: 100%;
}

.no-image-placeholder {
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 12px; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-placeholder);
}
.no-image-placeholder .el-icon { font-size: 40px; }
.no-image-placeholder span { margin-top: 12px; font-size: 0.9rem; }

.details-grid {
  display: flex; /* ★수정★ Grid에서 Flex로 변경하여 한 줄 레이아웃 강제 */
  flex-wrap: wrap; /* 화면이 매우 좁아질 경우를 대비해 줄바꿈 허용 */
  gap: 10px;
  /* background-color와 padding은 개별 아이템으로 이동 */
}
.detail-item {
  align-items: center;
  background-color: var(--el-fill-color-lighter);
  padding: 10px 16px;
  border-radius: 8px;
  flex: 1; /* 남은 공간을 동일하게 나눠가짐 */
}
.label-with-icon {
  justify-content: center;  /* 위아래 가운데 정렬 */
  align-items: center;
  display: flex;
  gap: 4px;
}
.item-icon {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
.item-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.item-text .label { font-size: 0.8rem; color: var(--el-text-color-secondary); }
.item-text .value { font-size: 0.95rem; font-weight: 500; color: var(--el-text-color-primary); }
.item-text .value-link { font-size: 0.95rem; font-weight: 500; color: var(--el-color-primary); text-decoration: none; }
.item-text .value-link:hover { text-decoration: underline; }

.memo-text {
  font-size: 0.95rem; line-height: 1.8;
  color: var(--el-text-color-regular); white-space: pre-wrap;
  word-break: keep-all; background-color: var(--el-fill-color-light);
  padding: 16px; border-radius: 8px; margin: 0;
}

.empty-detail {
  margin: auto; color: var(--el-text-color-placeholder);
}

</style>