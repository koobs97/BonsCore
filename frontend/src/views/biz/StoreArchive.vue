<script setup lang="ts">
import { ref, onMounted, computed, watch, onUnmounted, nextTick } from 'vue';
import { Plus, Edit, Delete, Search, Link, Calendar, StarFilled, Picture, ArrowLeftBold, ArrowRightBold } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import StoreFormDialog from '@/components/biz/StoreFormDialog.vue';
import { Api } from '@/api/axiosInstance';
import { ApiUrls } from '@/api/apiUrls';
import { userStore } from "@/store/userStore";
import { Dialogs } from "@/common/dialogs";
import { useI18n } from 'vue-i18n';

// --- i18n 설정 ---
const { t, locale } = useI18n();

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

// Tooltip을 위한 참조(ref)
const storeNameRef = ref<HTMLElement | null>(null); // 템플릿에서 h1 태그를 가리킬 변수
const isTitleOverflowing = ref(false); // 이름이 잘렸는지 여부를 저장할 변수 (기본값: false)

/** 현재 메인에 보여줄 이미지의 인덱스 */
const currentImageIndex = ref(0);

const formatDateToMonthDay = (date: any): string => {
  if (!date) return '';
  // date가 'YYYY-MM-DD' 형식이므로 new Date()가 정상적으로 파싱합니다.
  const d = new Date(date);
  if (isNaN(d.getTime())) {
    // 만약에라도 잘못된 날짜 형식이 들어올 경우를 대비
    return '??-??';
  }
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${month}-${day}`;
};

const fetchStores = async () => {
  isLoading.value = true;
  try {
    const payload = {};
    const config = {
      headers: {
        'Accept-Language': locale.value
      }
    };
    const response = await Api.post(ApiUrls.GET_GOURMET_RECORDS, payload, true, config);

    // 서버에서 받은 데이터를 클라이언트에서 사용하기 좋은 형태로 가공합니다.
    const formattedStores = response.data.map((store: any) => {
      // visitDate가 배열(Array) 형태인지 먼저 확인합니다.
      if (Array.isArray(store.visitDate) && store.visitDate.length >= 3) {
        const [year, month, day] = store.visitDate;
        // 'YYYY-MM-DD' 형식의 문자열로 변환합니다.
        const formattedDate = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        // 기존 store 객체를 복사하고, visitDate만 변환된 값으로 덮어씁니다.
        return { ...store, visitDate: formattedDate };
      }
      // 만약 배열이 아니라면 (예외 상황 대비), 기존 데이터를 그대로 반환합니다.
      return store;
    });

    // 가공이 완료된 데이터로 stores 상태를 업데이트합니다.
    stores.value = formattedStores;

    // 아래의 기존 로직은 그대로 유지됩니다.
    if (stores.value.length > 0) {
      if (!activeStoreId.value || !stores.value.some(s => s.id === activeStoreId.value)) {
        activeStoreId.value = stores.value[0].id;
      }
    } else {
      activeStoreId.value = null;
    }
  } catch (error) { } finally { isLoading.value = false; }
};

const handleDelete = async (storeId: number) => {

  await Dialogs.customConfirm(
      t('archive.dialog.deleteConfirmTitle'),
      t('archive.dialog.deleteConfirmText'),
      t('archive.dialog.deleteButton'),
      t('archive.dialog.cancelButton'),
      '460px',
  )

  ElMessage.success(t('archive.messages.deleteSuccess'));
  await fetchStores();

};

// Tooltip을 위한 함수
const checkTitleOverflow = () => {
  // nextTick: 화면이 완전히 그려진 후 코드를 실행하도록 보장
  nextTick(() => {
    const element = storeNameRef.value; // h1 태그를 가져옵니다.
    if (element) {
      // 요소의 실제 전체 너비(scrollWidth)가 화면에 보이는 너비(clientWidth)보다 크면 잘린 것으로 판단
      isTitleOverflowing.value = element.scrollWidth > element.clientWidth;
    }
  });
};

onMounted(() => {
  fetchStores();
  // 창 크기가 변경될 때마다 제목 잘림 여부를 다시 확인
  window.addEventListener('resize', checkTitleOverflow);
});

onUnmounted(() => {
  // 컴포넌트가 사라질 때 이벤트 리스너를 제거하여 메모리 누수 방지
  window.removeEventListener('resize', checkTitleOverflow);
});

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

watch(locale, () => {
  fetchStores();
});

// --- Watchers 및 Dialog 함수 (수정 없음) ---
watch(selectedStore, () => {
  // 가게 선택이 바뀌면, 이미지 인덱스를 첫 번째로 초기화
  currentImageIndex.value = 0;

  // 가게가 변경될 때마다 제목이 잘렸는지 새로 확인합니다.
  checkTitleOverflow();
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
  // fetchStores에서 이미 모든 날짜 형식이 'YYYY-MM-DD'로 변환되었으므로,
  // 여기서는 어떠한 추가 변환도 필요 없습니다. 그냥 데이터를 그대로 전달하면 됩니다.
  dialogState.value = {
    visible: true,
    isEditMode: true,
    initialData: { ...store } // 가공이 필요 없는 깔끔한 데이터
  };
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
        <h2 class="list-title">{{ t('archive.title') }}</h2>
        <el-button
            :icon="Plus"
            circle
            @click="openAddDialog" />
      </div>
      <div class="search-wrapper">
        <el-input
            v-model="searchQuery"
            :placeholder="t('archive.searchPlaceholder')"
            :prefix-icon="Search"
            clearable />
      </div>
      <div class="store-list">
        <el-skeleton :rows="6" animated v-if="isLoading" />
        <template v-else>
          <div
              v-for="store in filteredStores"
              :key="store.id"
              class="list-item"
              :class="{ active: activeStoreId === store.id }"
              @click="activeStoreId = store.id">
            <div class="item-info">
              <span class="item-name">{{ store.name }}</span>
              <span class="item-category">{{ store.category }}</span>
            </div>
            <span class="item-date">{{ formatDateToMonthDay(store.visitDate) }}</span>
          </div>
          <el-empty
              v-if="!filteredStores.length"
              :description="t('archive.noResults')"
              :image-size="60" />
        </template>
      </div>
    </div>

    <div class="detail-panel">
      <div v-if="selectedStore" class="detail-wrapper">
        <header class="detail-header">
          <el-tag type="info" round effect="plain" size="small">{{ selectedStore.category }}</el-tag>
          <div class="title-group">
            <el-tooltip
                effect="dark"
                :content="selectedStore.name"
                placement="top"
                :disabled="!isTitleOverflowing"
                :teleported="true"
                popper-class="custom-title-tooltip"
            >
              <h1 class="store-name" ref="storeNameRef">{{ selectedStore.name }}</h1>
            </el-tooltip>
          </div>
          <div class="actions-group">
            <el-button
                :icon="Edit"
                text
                circle
                @click="openEditDialog(selectedStore)"
                :title="t('archive.details.edit')" />
            <el-button
                type="danger"
                :icon="Delete"
                text
                circle
                @click="handleDelete(selectedStore.id)"
                :title="t('archive.details.delete')" />
          </div>
        </header>

        <main class="detail-body">
          <section class="gallery-section">
            <template v-if="selectedStore.images && selectedStore.images.length > 0">
              <div class="gallery-section">
                <div class="main-image-container">
                  <transition name="fade" mode="out-in">
                    <el-image
                        :key="currentMainImageUrl"
                        :src="currentMainImageUrl"
                        fit="cover"
                        class="main-image"
                        :preview-src-list="selectedStoreImageUrls"
                        :initial-index="currentImageIndex"
                        hide-on-click-modal
                        :preview-teleported="true"
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
            <div class="details-grid">
              <div class="detail-item">

                <div class="item-text">
                  <div class="label-with-icon">
                    <span class="label">{{ t('archive.details.visitDate') }}</span>
                    <el-icon class="item-icon"><Calendar /></el-icon>
                  </div>
                  <span class="value">{{ new Date(selectedStore.visitDate).toLocaleDateString('ko-KR') }}</span>
                </div>
              </div>
              <div class="detail-item">
                <div class="item-text">
                  <div class="label-with-icon">
                    <span class="label">{{ t('archive.details.rating') }}</span>
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
                    <span class="label">{{ t('archive.details.referenceLink') }}</span>
                    <el-icon class="item-icon"><Link /></el-icon>
                  </div>
                  <a :href="selectedStore.referenceUrl"
                      target="_blank"
                      class="value-link"
                  >
                    {{ t('archive.details.link') }}
                  </a>
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
  color: var(--el-text-color-regular);
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
  background-color: var(--el-bg-color);
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
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 12px; /* (추가) 자식 요소들 사이의 간격 */
  padding: 18px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 12px;
}
.title-group {
  display: flex;
  flex-direction: column; /* (변경) 제목과 태그를 수직으로 정렬할 수도 있습니다. 여기서는 수평을 유지합니다. */
  gap: 4px;
  /* (추가) 남는 공간을 모두 차지하도록 설정 */
  flex-grow: 1;
  /* (추가/중요) 자식 요소의 text-overflow가 작동하도록 하기 위한 필수 속성 */
  min-width: 0;
}

.store-name {
  font-size: 1.6rem; /* (조정) 약간 줄여서 공간 확보 */
  font-weight: 700;
  line-height: 1.3;
  margin: 0;
  color: var(--el-text-color-primary);
  font-family: 'Poppins', sans-serif;

  /* (추가) 이름이 길어질 경우 줄임표(...) 처리 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.actions-group {
  /* (추가) 이 그룹이 절대 줄어들지 않도록 설정 */
  flex-shrink: 0;
  /* (추가) 버튼들이 항상 오른쪽에 붙도록 자동 마진 설정 */
  margin-left: auto;
  display: flex;
  align-items: center;
}

.actions-group .el-button {
  font-size: 18px;
  color: var(--el-text-color-secondary);
  outline: none;
}

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
  overflow-x: scroll;
  overflow-y: hidden;
  padding-bottom: 2px; /* 스크롤바 공간 확보 */
  scrollbar-gutter: stable;
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
  background-color: var(--el-fill-color-light);
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
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.detail-item {
  align-items: center;
  padding: 10px 16px;
  border-radius: 8px;
  flex: 1; /* 남은 공간을 동일하게 나눠가짐 */
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  border: 1px solid var(--el-border-color-lighter);
  font-size: 0.85rem;
  font-weight: 500;
  transition: all 0.2s ease;
  text-align: left;
  display: flex;
  flex-direction: column;
  justify-content: center;
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
  height: 50px;
  font-size: 0.95rem;
  line-height: 1.8;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: keep-all;
  background-color: var(--el-fill-color-light);
  padding: 16px;
  border-radius: 8px;
  margin: 0;
  border: 1px solid var(--el-border-color-lighter);
  font-weight: 500;
  transition: all 0.2s ease;
  overflow-y: auto;
}

.empty-detail {
  margin: auto; color: var(--el-text-color-placeholder);
}

</style>
<style>
.custom-title-tooltip {
  /* 메인 색상을 배경으로 사용 */
  background: var(--el-color-primary, #001233) !important;
  /* 텍스트는 밝은 배경색 계열을 사용하여 가독성 확보 */
  color: var(--el-bg-color, #FFFAF0) !important;

  padding: 8px 12px;
  border-radius: 6px;
  /* 테두리는 배경보다 살짝 어두운 색으로 설정하여 입체감 부여 */
  border: 1px solid var(--el-color-primary-dark-2, #002855);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
  /* 디자인 시스템의 그림자 스타일 적용 */
  box-shadow: var(--el-box-shadow-light);
}

.custom-title-tooltip .el-popper__arrow {
  /*
    화살표 자체의 너비와 높이를 테두리 두께만큼 늘려줍니다.
    기본 크기(8px) + 테두리(1px*2) = 10px
  */
  width: 10px;
  height: 10px;
}
.custom-title-tooltip .el-popper__arrow::before {


  /* 배경색과 테두리색은 테마 변수를 그대로 사용합니다. */
  background: var(--el-color-primary, #001233) !important;
  border-color: var(--el-color-primary-dark-2, #002855) !important;

  /* 테두리 스타일을 명확히 정의해 주어야 clip-path가 잘 적용됩니다. */
  border-style: solid;
  border-width: 1px;
}


/*
  다크 모드 툴팁 스타일
  html.dark 선택자를 사용하여 다크 모드 시 스타일을 덮어씁니다.
*/
html.dark .custom-title-tooltip {
  /* 다크 모드에서는 primary 색상이 밝은 색이므로 그대로 사용 */
  background: var(--el-color-primary, #F2F2F2) !important;
  /* 텍스트는 다크 모드의 어두운 primary 계열 색상 사용 */
  color: var(--el-color-primary-dark-2, #002855) !important;

  border-color: var(--el-border-color-darker, #cdd0d6);
  box-shadow: var(--el-box-shadow-dark);
}

html.dark .custom-title-tooltip .el-popper__arrow::before {
  background: var(--el-color-primary, #F2F2F2) !important;
  border-color: var(--el-color-primary, #F2F2F2) !important;
}
</style>