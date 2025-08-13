<template>
  <div class="activity-log-container">

    <!-- 검색 패널 -->
    <el-card class="search-panel" shadow="never">
      <el-form :model="searchParams" size="small" inline style="text-align: left;">
        <div>
          <el-tag type="info" size="small" style="margin-right: 65px;">사용자 활동 로그</el-tag>
          <el-form-item>
            <el-date-picker
                v-model="searchParams.dateRange"
                type="daterange"
                range-separator="-"
                start-placeholder="시작일시"
                end-placeholder="종료일시"
                format="YYYY-MM-DD"
                value-format="YYYYMMDD"
                :size="'small'"
                style="width: 218px"
            />
          </el-form-item>
          <el-form-item>
            <div class="search-buttons">
              <el-button type="primary" :icon="Search" @click="onSearch" size="small">조회</el-button>
              <el-button :icon="Refresh" @click="onReset" size="small">초기화</el-button>
              <el-button :icon="FullScreen" @click="openFullScreenGrid" size="small">크게 보기</el-button>
            </div>
          </el-form-item>
        </div>
        <el-form-item>
          <el-input v-model="searchParams.userId" placeholder="사용자 ID" clearable style="width: 120px; margin-left: 173px;" />
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchParams.activityType" placeholder="활동 유형" clearable style="width: 110px">
            <el-option
                v-for="item in activityTypeList"
                :key="item.activityType"
                :label="item.activityType"
                :value="item.activityType"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchParams.activityResult" placeholder="결과" clearable style="width: 90px">
            <el-option
                v-for="item in activityResultList"
                :key="item.activityResult"
                :label="item.activityResult"
                :value="item.activityResult"
            />
          </el-select>
        </el-form-item>

      </el-form>
    </el-card>

    <!-- 3. 데이터 그리드 (기존) -->
    <div class="grid-container">
      <ag-grid-vue
          class="ag-theme-alpine"
          :theme="'legacy'"
          style="width: 100%; height: 100%;"
          :columnDefs="colDefs"
          :rowData="rowData"
          :defaultColDef="defaultColDef"
          :pagination="true"
          :paginationPageSize="10"
          :paginationPageSizeSelector="[10, 20, 50]"
          :localeText="localeText"
          @grid-ready="onGridReady"
      >
      </ag-grid-vue>
    </div>

    <!-- [핵심] 4. 그리드를 크게 보여줄 모달 (el-dialog) -->
    <el-dialog
        v-model="isModalVisible"
        title="사용자 활동 로그 (전체 화면)"
        width="70%"
        height="60%"
        top="10vh"
        destroy-on-close
        class="fullscreen-dialog"
    >
      <!-- 모달 내부에도 똑같은 그리드 -->
      <ag-grid-vue
          class="ag-theme-alpine"
          :theme="'legacy'"
          style="width: 100%; height: 75vh;"
          :columnDefs="colDefs"
          :rowData="rowData"
          :defaultColDef="defaultColDef"
          :pagination="true"
          :paginationPageSize="20"
          :paginationPageSizeSelector="[20, 50, 100]"
          :localeText="localeText"
          @grid-ready="onModalGridReady"
      >
      </ag-grid-vue>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { AgGridVue } from 'ag-grid-vue3';
import { ElMessage, ElLoading, ElDialog } from 'element-plus';
import { Monitor, Search, Refresh, FullScreen } from '@element-plus/icons-vue';
import {Api} from "@/api/axiosInstance.js";
import {ApiUrls} from "@/api/apiUrls.js";

// --- 상태 변수 및 Ref ---
const isModalVisible = ref(false); // 모달 표시 여부 상태
const modalGridApi = ref(null);    // 모달 내부 그리드의 API

const gridApi = ref(null);
const searchParams = reactive({ dateRange: [], userId: '', activityType: '', activityResult: '' });
const activityTypeList = ref([])
const activityResultList = ref([])

const rowData = ref([]);
const defaultColDef = { resizable: true, sortable: true, filter: true };

const localeText = reactive({
  filterOoo: '필터...',
  applyFilter: '필터 적용',
  resetFilter: '필터 초기화',
  pageSizeSelectorLabel: '페이지 크기:',
  page: '페이지',
  of: '/',
  to: '-',
  firstPage: '첫 페이지',
  lastPage: '마지막 페이지',
  nextPage: '다음',
  previousPage: '이전',
  rowCount: '개',
  noRowsToShow: '표시할 데이터가 없습니다',
  pinColumn: '열 고정',
  autosizeThiscolumn: '열 너비 자동 조정',
});

// --- 그리드 컬럼 정의 ---
const colDefs = ref([
  { headerName: 'ID', field: 'logId', width: 90 },
  { headerName: '시간', field: 'createdAt', width: 190,
    valueFormatter: p => p.value ? new Date(p.value).toLocaleString('ko-KR', {
      year: '2-digit', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
    }) : ''
  },
  { headerName: '사용자ID', field: 'userId', width: 180, cellStyle: { 'text-align': 'left' } },
  { headerName: '유형', field: 'activityType', width: 150, cellStyle: { 'text-align': 'left' } },
  {
    headerName: '결과',
    field: 'activityResult',
    width: 80,
    cellRenderer: (params) => {
      if (params.value === 'SUCCESS') return `<span style="color: #67c23a;">● 성공</span>`;
      if (params.value === 'FAILURE') return `<span style="color: #f56c6c;">● 실패</span>`;
      return params.value;
    }
  },
  { headerName: '요청 IP', field: 'requestIp', width: 140 },
  { headerName: '요청 URI', field: 'requestUri', width: 200, tooltipValueGetter: (p) => p.value },
  { headerName: '메소드', field: 'requestMethod', width: 100 },
  { headerName: '에러 메시지', field: 'errorMessage', width: 150, tooltipValueGetter: (p) => p.value },
  { headerName: 'User-Agent', field: 'userAgent', width: 1000, tooltipValueGetter: (p) => p.value },
]);

// --- 함수 ---
const onGridReady = (params) => { gridApi.value = params.api; };

// 모달을 여는 함수
const openFullScreenGrid = () => {
  isModalVisible.value = true;
};

// 모달 그리드가 준비되었을 때 호출될 함수
const onModalGridReady = (params) => {
  modalGridApi.value = params.api;
  // 메인 그리드의 컬럼 상태(정렬, 필터 등)를 모달 그리드에 동기화
  if(gridApi.value) {
    const mainGridState = gridApi.value.getColumnState();
    if(mainGridState) {
      modalGridApi.value.applyColumnState({ state: mainGridState, applyOrder: true });
    }
  }
}

const getActivity = async () => {
  const response = await Api.post(ApiUrls.GET_ACTIVITY, { });
  console.log(response)

  activityTypeList.value = response.data.activityTypeList
  activityResultList.value = response.data.activityResultList
}

const fetchLogs = async () => {
  const loadingInstance = ElLoading.service({ target: '.grid-container', text: '로딩 중...' });
  try {
    const apiParams = {
      startDate: searchParams.dateRange ? searchParams.dateRange[0] : "",
      endDate: searchParams.dateRange ? searchParams.dateRange[1] : "",
      userId: searchParams.userId,
      activityType: searchParams.activityType,
      activityResult: searchParams.activityResult,
    };
    await new Promise(resolve => setTimeout(resolve, 500));
    rowData.value = await generateMockData(apiParams);

  } catch (error) {
    console.error("로그 데이터 조회 실패:", error);
    ElMessage.error('데이터 조회 중 오류가 발생했습니다.');
  } finally {
    loadingInstance.close();
  }
};

const onSearch = () => fetchLogs();
const onReset = () => {
  Object.assign(searchParams, { dateRange: [], userId: '', activityType: '', activityResult: '' });
  fetchLogs();
};

onMounted(() => {
  getActivity();
  fetchLogs();
});

const dateShortcuts = [
  { text: '오늘', value: () => { const end = new Date(); const start = new Date(); start.setHours(0, 0, 0, 0); return [start, end] } },
  { text: '어제', value: () => { const end = new Date(); const start = new Date(); start.setDate(start.getDate() - 1); end.setDate(end.getDate() - 1); start.setHours(0,0,0,0); end.setHours(23,59,59,999); return [start, end] } },
  { text: '최근 7일', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 7); return [start, end] } },
]

// --- 테스트용 목업 데이터 생성 함수 ---
const generateMockData = async (params) => {
  console.log(params)
  const response = await Api.post(ApiUrls.GET_LOGS, params);
  console.log(response)
  return response.data;
};
</script>

<style scoped>
/* 전체 컨테이너 스타일 */
.activity-log-container {
  padding: 10px;
  display: flex;
  flex-direction: column;
  height: 380px;
  background-color: #fff;
  font-size: 12px;
}
.el-card {
  --el-card-border-color: var(--el-border-color-light);
  --el-card-border-radius: 4px;
  --el-card-padding: 4px;
  --el-card-bg-color: var(--el-fill-color-blank);
  background-color: var(--el-card-bg-color);
  border: 1px solid var(--el-card-border-color);
  border-radius: var(--el-card-border-radius);
  color: var(--el-text-color-primary);
  overflow: hidden;
  transition: var(--el-transition-duration);
}
.page-header h5 {
  font-size: 1.1rem;
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
}

/* 검색 패널 스타일 */
.search-panel {
  margin-bottom: 4px;
  padding: 4px;
  background-color: #fdfdfd;
  border: 1px solid #ebeef5;
}
.el-form--inline .el-form-item {
  margin-right: 8px;
  margin-bottom: 8px;
}
.search-buttons {
  display: flex;
  gap: 2px;
}

/* 그리드 컨테이너 스타일 */
.grid-container {
  flex-grow: 1;
  width: 100%;
}

:deep(.el-input), :deep(.el-select), :deep(.el-date-editor) {
  vertical-align: middle;
}

/* --- 페이지네이션 커스텀 스타일 (기존 코드 유지) --- */
:deep(.ag-paging-panel) {
  height: 36px !important;
  padding: 0 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: nowrap;
  overflow: hidden;
}
:deep(.ag-paging-row-summary-panel) {
  display: flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}
:deep(.ag-page-size-selector) {
  display: flex;
  align-items: center;
  gap: 6px;
}
:deep(.ag-paging-page-summary-panel) {
  display: flex;
  align-items: center;
  gap: 4px;
}
:deep(.ag-paging-panel > *) {
  margin-left: 0 !important;
  margin-right: 0 !important;
}
:deep(.ag-paging-description) {
  display: none !important;
}
:deep(.ag-paging-button) {
  width: 12px !important;
  height: 12px !important;
  padding: 0 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  border-radius: 4px;
}
:deep(.ag-paging-button .ag-icon) {
  font-size: 16px !important;
  box-shadow: none !important;
  filter: none !important;
  transform: none !important;
  opacity: 1 !important;
}

/* [핵심] 모달(Dialog) 스타일 커스터마이징 */
:deep(.fullscreen-dialog .el-dialog__header) {
  padding-bottom: 10px;
  margin-right: 0;
}

:deep(.fullscreen-dialog .el-dialog__body) {
  padding: 0 !important; /* 내부 패딩을 제거하여 그리드가 꽉 차도록 함 */
}
</style>