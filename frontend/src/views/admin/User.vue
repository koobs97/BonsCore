<template>
  <div class="user-management-container">

    <!-- 검색 패널 -->
    <el-card class="search-panel" shadow="never">
      <el-form :model="searchParams" size="small" inline style="text-align: left; background-color: var(--el-bg-color);">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <el-tag type="info" size="small">사용자 관리</el-tag>
          <div>
            <el-button type="primary" :icon="Search" @click="onSearch" size="small" style="color: var(--el-bg-color);">조회</el-button>
            <el-button :icon="Refresh" @click="onReset" size="small">초기화</el-button>
          </div>
        </div>

        <el-card shadow="never" style="margin-top: 4px;">
          <el-form-item style="margin-top: 8px;" @keyup.enter="onSearch">
            <el-input v-model="searchParams.userId" placeholder="사용자 ID" clearable style="width: 150px; margin-right: 4px;" />
            <el-input v-model="searchParams.userName" placeholder="이름" clearable style="width: 120px; margin-right: 4px;" />
            <el-input v-model="searchParams.email" placeholder="이메일" clearable style="width: 220px; margin-right: 4px;" />
            <el-select v-model="searchParams.accountStatus" placeholder="계정 상태" clearable style="width: 120px; margin-right: 6px;">
              <el-option label="정상" value="active"/>
              <el-option label="휴면" value="locked"/>
              <el-option label="탈퇴" value="withdrawn"/>
            </el-select>
          </el-form-item>
        </el-card>
      </el-form>
    </el-card>

    <!-- 데이터 그리드 -->
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { AgGridVue } from 'ag-grid-vue3';
import { ElLoading } from 'element-plus';
import { Search, Refresh } from '@element-plus/icons-vue';
import {Api} from "@/api/axiosInstance.js";
import {ApiUrls} from "@/api/apiUrls.js";

// --- 상태 변수 ---
const gridApi = ref(null);
const searchParams = reactive({ userId: '', userName: '', email: '', accountStatus: '' });
const rowData = ref([]);
const defaultColDef = { resizable: true, sortable: true, filter: true };

// AG Grid 한글 로케일 설정
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
  { headerName: '사용자ID', field: 'userId', width: 150, cellStyle: { 'text-align': 'left' } },
  { headerName: '이름', field: 'userName', width: 110 },
  { headerName: '이메일', field: 'email', width: 190, cellStyle: { 'textAlign': 'left' } },
  { headerName: '휴대폰 번호', field: 'phoneNumber', width: 130 },
  { headerName: '가입일', field: 'createdAt', width: 120 },
  {
    headerName: '계정 상태', field: 'accountstatus', width: 110,
    cellRenderer: (params) => {
      if (params.data.withdrawn === 'Y') return `<span style="color: #f56c6c;">● 탈퇴</span>`;
      if (params.data.accountLocked === 'Y') return `<span style="color: #e6a23c;">● 휴면</span>`;
      return `<span style="color: #67c23a;">● 정상</span>`;
    }
  },
]);

// --- 함수 ---
const onGridReady = (params) => { gridApi.value = params.api; };

const fetchUsers = async () => {
  const loadingInstance = ElLoading.service({ target: '.grid-container', text: '로딩 중...' });
  try {
    let accountLocked = '';
    let withdrawn = '';
    if(searchParams.accountStatus === 'active'){
      accountLocked = 'N';
      withdrawn = 'N';
    }
    if(searchParams.accountStatus === 'locked'){
      accountLocked = 'Y';
      withdrawn = 'N';
    }
    if(searchParams.accountStatus === 'withdrawn'){
      accountLocked = 'Y';
      withdrawn = 'Y';
    }

    const apiParams = {
      userId: searchParams.userId,
      userName: searchParams.userName,
      email: searchParams.email,
      accountLocked: accountLocked,
      withdrawn: withdrawn,
    };
    const response = await Api.post(ApiUrls.GET_USER_INFOS, apiParams);
    console.log(response.data)
    rowData.value = response.data;
  } catch (error) { } finally { loadingInstance.close(); }
};
const onSearch = () => fetchUsers();
const onReset = () => {
  Object.assign(searchParams, { userId: '', userName: '', email: '', accountStatus: '' });
  fetchUsers();
};
onMounted(() => { fetchUsers(); });
</script>

<style>
@import "ag-grid-community/styles/ag-grid.css";
@import "ag-grid-community/styles/ag-theme-alpine.css";
</style>

<style scoped>
/* 전체 컨테이너 스타일 */
.user-management-container {
  display: flex;
  flex-direction: column;
  height: 620px;
  width: 820px;
  background-color: var(--el-bg-color);
  font-size: 12px;
  margin: 4px 0 0 0;
  box-sizing: border-box;
}
.el-card {
  --el-card-border-color: var(--el-border-color-light);
  --el-card-border-radius: 4px;
  --el-card-padding: 4px;
  --el-card-bg-color: var(--el-fill-color-blank);
  background-color: var(--el-card-bg-color);
  border: 1px solid var(--el-card-border-color);
  border-radius: 4px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  transition: var(--el-transition-duration);
}

/* 검색 패널 스타일 */
.search-panel {
  margin-bottom: 4px;
  padding: 4px;
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-text-color-disabled);
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

/* 페이지네이션 커스텀 스타일 */
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
</style>

<style>
/* =================================================================== */
/* ============== AG Grid 다크 모드 전역 스타일 (scoped 없음) ============== */
/* =================================================================== */
html.dark .ag-theme-alpine {
  /* 기본 배경 및 텍스트 색상 */
  --ag-background-color: var(--el-bg-color-overlay);
  --ag-data-color: var(--el-text-color-primary);
  --ag-foreground-color: var(--el-text-color-primary);
  --ag-secondary-foreground-color: var(--el-text-color-secondary);

  /* 헤더 스타일 */
  --ag-header-background-color: var(--el-bg-color);
  --ag-header-foreground-color: var(--el-text-color-primary);
  --ag-header-cell-hover-background-color: var(--el-fill-color-light);
  --ag-header-cell-moving-background-color: var(--el-fill-color-darker);

  /* 행(Row) 스타일 */
  --ag-row-background-color: var(--el-bg-color-overlay);
  --ag-odd-row-background-color: #1d1e22;
  --ag-row-hover-color: var(--el-fill-color);

  /* 테두리 및 구분선 색상 */
  --ag-border-color: var(--el-border-color);
  --ag-separator-color: var(--el-border-color-light);
  --ag-row-border-color: var(--el-border-color-lighter);

  /* 입력 컨트롤(필터 등) 스타일 */
  --ag-input-background-color: var(--el-fill-color-blank);
  --ag-input-border-color: var(--el-border-color);
  --ag-input-disabled-background-color: var(--el-disabled-bg-color);
  --ag-input-disabled-border-color: var(--el-disabled-border-color);
  --ag-input-focus-border-color: var(--el-color-primary);

  /* 체크박스 스타일 */
  --ag-checkbox-background-color: var(--el-fill-color-blank);
  --ag-checkbox-checked-color: var(--el-color-primary);

  /* 툴팁 스타일 */
  --ag-tooltip-background-color: var(--el-color-black);
  --ag-tooltip-foreground-color: var(--el-color-white);

  /* 페이지네이션 텍스트 및 아이콘 색상 */
  --ag-secondary-color: var(--el-text-color-secondary);
}

/* 다크 모드일 때 페이지네이션 아이콘 색상 조정 */
html.dark .ag-paging-button .ag-icon {
  color: var(--el-text-color-secondary);
}
html.dark .ag-paging-button:not(.ag-disabled) .ag-icon:hover {
  color: var(--el-text-color-primary);
}
</style>