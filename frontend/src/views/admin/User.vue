<template>
  <div class="user-management-container">

    <!-- 탭 레이아웃 적용 -->
    <el-tabs v-model="activeTab" class="main-tabs">

      <!-- [탭 1] 사용자 관리 (기존 기능 유지 + 헤더 디자인만 변경) -->
      <el-tab-pane label="사용자 관리" name="user" class="full-height-tab">

        <!-- 1. 헤더 (참고 화면 스타일로 디자인 변경) -->
        <div class="top-search-bar">
          <!-- 타이틀 -->
          <div class="section-left">
            <span class="page-title">사용자 관리</span>
            <el-tag type="info" size="small" effect="plain" round class="count-tag">
              Total {{ rowData.length }}
            </el-tag>
          </div>

          <!-- 검색 조건 (기존 searchParams와 바인딩) -->
          <div class="section-center">
            <el-input v-model="searchParams.userId" placeholder="사용자 ID" size="small" class="filter-input" clearable @keyup.enter="onSearch" />
            <el-input v-model="searchParams.userName" placeholder="이름" size="small" class="filter-input-short" clearable @keyup.enter="onSearch" />
            <el-input v-model="searchParams.email" placeholder="이메일" size="small" class="filter-input-long" clearable @keyup.enter="onSearch" />
            <el-select v-model="searchParams.accountStatus" placeholder="계정 상태" size="small" class="filter-select" clearable>
              <el-option label="정상" value="active"/>
              <el-option label="휴면" value="locked"/>
              <el-option label="탈퇴" value="withdrawn"/>
            </el-select>
          </div>

          <!-- 버튼 -->
          <div class="section-right">
            <el-button-group>
              <el-button plain size="small" :icon="Search" @click="onSearch">조회</el-button>
              <el-button size="small" :icon="Refresh" @click="onReset" title="초기화"></el-button>
            </el-button-group>
          </div>
        </div>

        <!-- 2. 그리드 (기존 코드 100% 유지) -->
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
      </el-tab-pane>

      <!-- [탭 2] 보안 질문 관리 (새로 추가된 기능) -->
      <el-tab-pane label="보안 질문 설정" name="security" class="full-height-tab">

        <!-- 헤더 -->
        <div class="top-search-bar">
          <div class="section-left">
            <span class="page-title">질문 목록</span>
          </div>
          <div class="section-center">
            <!-- 보안 질문 전용 검색어 -->
            <el-input v-model="sqSearchText" placeholder="질문 내용 검색" size="small" style="width: 300px;" clearable @keyup.enter="fetchSqData" >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
          </div>
          <div class="section-right">
            <el-button plain size="small" :icon="Search" @click="fetchSqData">조회</el-button>
            <el-divider direction="vertical" />
            <el-button type="primary" size="small" :icon="Plus" @click="openSqDialog(null)">등록</el-button>
          </div>
        </div>

        <!-- 보안 질문 그리드 -->
        <div class="grid-container">
          <ag-grid-vue
              class="ag-theme-alpine"
              :theme="'legacy'"
              style="width: 100%; height: 100%;"
              :columnDefs="sqColDefs"
              :rowData="sqRowData"
              :defaultColDef="defaultColDef"
              :pagination="true"
              :paginationPageSize="10"
              :paginationPageSizeSelector="[10, 20, 50]"
              :localeText="localeText"
              @grid-ready="onGridReady"
          >
          </ag-grid-vue>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- [다이얼로그] 보안 질문 등록/수정 -->
    <el-dialog
        v-model="sqDialogVisible"
        :title="sqIsEdit ? '보안 질문 수정' : '새 보안 질문 등록'"
        width="500px"
        align-center
    >
      <el-form :model="sqForm" ref="sqFormRef" label-width="90px">
        <el-form-item label="코드" prop="questionCode" :rules="[{ required: true, message: '필수', trigger: 'blur' }]">
          <el-input v-model="sqForm.questionCode" :disabled="sqIsEdit" placeholder="예: Q10" maxlength="10" />
        </el-form-item>
        <el-form-item label="질문 내용" prop="questionText" :rules="[{ required: true, message: '필수', trigger: 'blur' }]">
          <el-input v-model="sqForm.questionText" type="textarea" :rows="3" />
        </el-form-item>
        <div style="display:flex; gap:10px;">
          <el-form-item label="순서" style="flex:1">
            <el-input-number v-model="sqForm.displayOrder" :min="0" controls-position="right" style="width:100%" />
          </el-form-item>
          <el-form-item label="사용" style="flex:1">
            <el-switch v-model="sqForm.useYn" active-value="Y" inactive-value="N" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="sqDialogVisible = false">취소</el-button>
        <el-button type="primary" @click="saveSqData">저장</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { AgGridVue } from 'ag-grid-vue3';
import { ElLoading, ElMessage, ElMessageBox } from 'element-plus';
import { Search, Refresh, Plus } from '@element-plus/icons-vue';
import { Api } from "@/api/axiosInstance.js";
import { ApiUrls } from "@/api/apiUrls.js";

// 탭 상태
const activeTab = ref('user');

// =================================================================
// 1. 기존 사용자 관리 로직 (건드리지 않음 / 헤더 연결만 수정)
// =================================================================
const gridApi = ref(null);
const searchParams = reactive({ userId: '', userName: '', email: '', accountStatus: '' });
const rowData = ref([]);
const defaultColDef = { resizable: true, sortable: true, filter: true };

// AG Grid 한글 로케일 (기존 유지)
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

// 그리드 컬럼 정의 (기존 유지)
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
    const response = await Api.get(ApiUrls.GET_USER_INFOS, apiParams);
    console.log(response.data)
    rowData.value = response.data;
  } catch (error) { } finally { loadingInstance.close(); }
};
const onSearch = () => fetchUsers();
const onReset = () => {
  Object.assign(searchParams, { userId: '', userName: '', email: '', accountStatus: '' });
  fetchUsers();
};

// =================================================================
// 2. [신규] 보안 질문 관리 로직
// =================================================================
const sqSearchText = ref('');
const sqRowData = ref([]);
const sqGridApi = ref(null);
const sqDialogVisible = ref(false);
const sqIsEdit = ref(false);
const sqFormRef = ref(null);
const sqForm = reactive({ questionCode: '', questionText: '', displayOrder: 0, useYn: 'Y' });

// Mock Data (SQL 기초데이터 반영)
const mockSqData = [
  { questionCode: 'Q1', questionText: '오래 기억하고 있는 첫사랑의 이름은 무엇인가요?', displayOrder: 10, useYn: 'Y' },
  { questionCode: 'Q2', questionText: '처음으로 키웠던 반려동물의 이름은 무엇인가요?', displayOrder: 20, useYn: 'Y' },
  { questionCode: 'Q3', questionText: '졸업한 초등학교의 이름은 무엇인가요?', displayOrder: 30, useYn: 'Y' },
  { questionCode: 'Q4', questionText: '가장 기억에 남는 여행지는 어디인가요?', displayOrder: 40, useYn: 'Y' },
  { questionCode: 'Q5', questionText: '나의 출생지는 어디인가요?', displayOrder: 50, useYn: 'Y' },
];

const sqColDefs = ref([
  { headerName: '코드', field: 'questionCode', width: 90, pinned:'left', cellStyle: { fontWeight:'bold', textAlign:'center'} },
  { headerName: '질문 내용', field: 'questionText', flex: 1, cellStyle: { textAlign: 'left' } },
  { headerName: '순서', field: 'displayOrder', width: 80, cellStyle: { textAlign:'center'} },
  {
    headerName: '사용', field: 'useYn', width: 80, cellStyle: { textAlign:'center'},
    cellRenderer: (p) => p.value === 'Y' ? '<span style="color:#67c23a">사용</span>' : '<span style="color:#909399">미사용</span>'
  },
  {
    headerName: '관리', width: 80, pinned:'right', sortable: false,
    cellRenderer: () => `<div style="cursor:pointer;"><i class="edit-btn">✎</i> <i class="del-btn" style="color:red; margin-left:8px;">✕</i></div>`
  }
]);

const onSqGridReady = (p) => { sqGridApi.value = p.api; fetchSqData(); };

// 조회
const fetchSqData = () => {
  // Mock Fetch
  if(!sqSearchText.value) sqRowData.value = [...mockSqData];
  else sqRowData.value = mockSqData.filter(d => d.questionText.includes(sqSearchText.value));
};

// 등록/수정 팝업
const openSqDialog = (row) => {
  sqDialogVisible.value = true;
  if(row) {
    sqIsEdit.value = true;
    Object.assign(sqForm, row);
  } else {
    sqIsEdit.value = false;
    Object.assign(sqForm, { questionCode: '', questionText: '', displayOrder: 0, useYn: 'Y' });
  }
};

// 저장
const saveSqData = async () => {
  if(!sqFormRef.value) return;
  await sqFormRef.value.validate((valid) => {
    if(valid) {
      if(sqIsEdit.value) {
        const idx = mockSqData.findIndex(d => d.questionCode === sqForm.questionCode);
        if(idx > -1) mockSqData[idx] = { ...sqForm };
      } else {
        if(mockSqData.some(d => d.questionCode === sqForm.questionCode)) {
          return ElMessage.error('중복된 코드입니다.');
        }
        mockSqData.push({ ...sqForm });
      }
      sqDialogVisible.value = false;
      fetchSqData();
      ElMessage.success('저장되었습니다.');
    }
  });
};

// 삭제/수정 클릭 처리
const onSqCellClicked = (params) => {
  const cls = params.event.target.className;
  if(cls === 'edit-btn') openSqDialog(params.data);
  if(cls.includes('del-btn')) {
    ElMessageBox.confirm('삭제하시겠습니까?').then(() => {
      const idx = mockSqData.findIndex(d => d.questionCode === params.data.questionCode);
      if(idx > -1) mockSqData.splice(idx, 1);
      fetchSqData();
      ElMessage.success('삭제되었습니다.');
    });
  }
};

onMounted(() => {
  fetchUsers();
});
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
/* 탭 스타일 조정 (꽉 채우기) */
.main-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
}
/* 1. 탭 헤더 영역: 배경 투명, 하단 라인 */
:deep(.el-tabs__header) {
  margin: 0 0 4px 0; /* 헤더와 내용 사이 간격 */
  background-color: var(--el-bg-color) !important;
  border-bottom: 1px solid var(--el-border-color-light); /* 연한 회색 줄 */
  padding: 0 4px; /* 좌우 여백 살짝 */
  height: 50px !important;
  border-radius: 2px !important;
}

/* 2. 탭 네비게이션(가로 줄) */
:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

/* 3. 개별 탭 아이템: 작고 심플하게 */
:deep(.el-tabs__item) {
  font-size: 14px !important;         /* 글자 작게 */
  font-weight: 500;        /* 굵기 보통 */
  color: #909399;          /* 기본 회색 */
  padding: 0 12px !important; /* 좌우 패딩 좁게 */
  height: 44px !important;
  line-height: 44px !important;
  border: none !important;
  margin-bottom: -1px;
}

/* 4. 선택된 탭: 진하게 & 색상 강조 */
:deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary); /* 메인 색상 */
  font-weight: 700;
  background-color: transparent !important; /* 배경색 제거 */
}

/* 5. 선택된 탭 밑줄 (파란 막대) */
:deep(.el-tabs__active-bar) {
  height: 2px !important;          /* 두께 고정 */
  background-color: var(--el-color-primary) !important;
  bottom: 0;          /* 바닥에 딱 붙임 */
  z-index: 10;                     /* 회색 줄보다 위에 표시 */
  border-radius: 2px 2px 0 0;      /* (선택) 윗부분 살짝 둥글게 */
}
:deep(.el-tabs__active-bar::before),
:deep(.el-tabs__active-bar::after) {
  display: none !important;
  content: none !important;
}

/* 6. 탭 내용 영역: 꽉 채우기 */
:deep(.el-tabs__content) {
  flex: 1;
  padding: 0 !important;
  display: flex;
  flex-direction: column;
}

/* (참고) 탭 내부 Pane 스타일 */
.full-height-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* 1. 참고 화면 스타일 적용한 헤더 */
.top-search-bar {
  height: 48px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  margin: 4px 0 4px 0;
}
.section-left {
  display: flex; align-items: center; gap: 8px; min-width: 130px;
}
.page-title { font-weight: 700; font-size: 14px; }

.section-center {
  flex: 1; display: flex; justify-content: center; gap: 6px;
}
.filter-input { width: 120px; }
.filter-input-short { width: 90px; }
.filter-input-long { width: 160px; }
.filter-select { width: 90px; }

.section-right { display: flex; align-items: center; }

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