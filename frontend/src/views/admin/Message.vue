<template>
  <div class="i18n-management-container">

    <!-- 1. 검색 패널 -->
    <div class="top-search-bar">
      <!-- 왼쪽: 타이틀 & 통계 -->
      <div class="section-left">
        <span class="page-title">다국어 관리</span>
        <el-tag type="info" size="small" effect="plain" round class="count-tag">
          Total {{ pivotRowData.length }}
        </el-tag>
      </div>

      <!-- 중앙: 검색창 (넓게 배치) -->
      <div class="section-center">
        <el-input
            v-model="searchParams.keyword"
            placeholder="Code 또는 Message 검색"
            size="small"
            clearable
            @keyup.enter="onSearch"
            class="compact-input"
        >
          <template #prefix>
            <el-icon class="search-icon"><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 오른쪽: 버튼 그룹 -->
      <div class="section-right">
        <el-button-group>
          <el-button plain size="small" :icon="Search" @click="onSearch">조회</el-button>
          <el-button size="small" :icon="Refresh" @click="onReset" title="초기화"></el-button>
        </el-button-group>
        <el-divider direction="vertical" style="height: 18px; margin: 0 8px;" />
        <el-button type="primary" size="small" :icon="Plus" @click="openDialog(null)">등록</el-button>
      </div>
    </div>

    <!-- 2. 데이터 그리드 -->
    <div class="grid-container">
      <ag-grid-vue
          class="ag-theme-alpine"
          :theme="'legacy'"
          style="width: 100%; height: 100%;"
          :columnDefs="colDefs"
          :rowData="pivotRowData"
          :defaultColDef="defaultColDef"
          :pagination="true"
          :paginationPageSize="15"
          :paginationPageSizeSelector="[15, 30, 50]"
          :localeText="localeText"
          rowSelection="single"
          @grid-ready="onGridReady"
          @cell-clicked="onCellClicked"
      >
      </ag-grid-vue>
    </div>

    <!-- 3. 등록/수정 다이얼로그 (세트 관리) -->
    <el-dialog
        v-model="dialogVisible"
        :title="isEditMode ? '다국어 세트 수정' : '새 다국어 세트 등록'"
        width="600px"
        align-center
        destroy-on-close
        :close-on-click-modal="false"
    >
      <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-width="110px"
          size="default"
          status-icon
      >
        <!-- Code Key -->
        <el-form-item label="Code Key" prop="code">
          <!-- 수정 모드일 때는 input disabled, 등록일 때는 autocomplete -->
          <el-input
              v-if="isEditMode"
              v-model="formData.code"
              disabled
          >
            <template #prefix><el-icon><Key /></el-icon></template>
          </el-input>

          <el-autocomplete
              v-else
              v-model="formData.code"
              :fetch-suggestions="querySearchGroup"
              placeholder="그룹을 선택하거나 직접 입력하세요 (ex: login.newKey)"
              class="w-full"
              style="width: 100%"
              trigger-on-focus
              clearable
          >
            <template #prefix><el-icon><Key /></el-icon></template>
            <template #default="{ item }">
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-weight: bold; color: var(--el-text-color-primary);">{{ item.value }}</span>
                <span style="font-size: 10px; color: #999; margin-left: 10px;">기존 그룹</span>
              </div>
            </template>
          </el-autocomplete>

          <span v-if="isEditMode" style="font-size: 11px; color: var(--el-color-warning); margin-top: 4px;">
            * 코드 키는 수정할 수 없습니다.
          </span>
          <span v-else style="font-size: 11px; color: var(--el-text-color-secondary); margin-top: 4px;">
            * 기존 그룹(Prefix)을 선택하면 뒤에 이어서 작성할 수 있습니다. (중복 불가)
          </span>
        </el-form-item>

        <el-divider content-position="left">메시지 입력</el-divider>

        <!-- Message KO -->
        <el-form-item label="한국어 (KO)" prop="messageKo">
          <el-input
              v-model="formData.messageKo"
              type="textarea"
              :rows="3"
              placeholder="한국어 메시지 입력"
          />
        </el-form-item>

        <!-- Message EN -->
        <el-form-item label="English (EN)" prop="messageEn">
          <el-input
              v-model="formData.messageEn"
              type="textarea"
              :rows="3"
              placeholder="Enter English message"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">취소</el-button>
          <el-button type="primary" @click="saveData">
            {{ isEditMode ? '전체 저장' : '일괄 등록' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { AgGridVue } from 'ag-grid-vue3';
import { ElLoading, ElMessage, ElMessageBox } from 'element-plus';
import { Search, Refresh, Plus, Key } from '@element-plus/icons-vue';
import { ApiUrls } from "@/api/apiUrls.js";
import { Api } from "@/api/axiosInstance.js";
import {Dialogs} from "@/common/dialogs.js";

// --- State Variables ---
const gridApi = ref(null);
const searchParams = reactive({ keyword: '' }); // Locale 제거 (항상 ALL)
const pivotRowData = ref([]); // 피벗(그룹핑)된 데이터
const existingPrefixes = ref([]); // 추출된 코드 그룹(Prefix) 목록
const defaultColDef = { resizable: true, sortable: true, filter: true };

// Dialog State
const dialogVisible = ref(false);
const isEditMode = ref(false);
const formRef = ref(null);
// Form 데이터 구조 변경: code 하나에 ko/en 메시지 포함
const formData = reactive({
  code: '',
  messageKo: '',
  messageEn: '',
  idKo: null,
  idEn: null
});

// --- [핵심] Custom Validator: 중복 체크 로직 ---
const validateCodeUnique = (rule, value, callback) => {
  if (!value) {
    return callback(new Error('Code Key를 입력해주세요.'));
  }

  // 수정 모드일 때는 중복 체크 건너뜀 (어차피 disabled)
  if (isEditMode.value) {
    return callback();
  }

  // pivotRowData에는 현재 화면에 로드된 모든 고유 Code들이 있음
  // some()을 사용하여 입력된 value와 정확히 일치하는 Code가 있는지 확인
  const isDuplicate = pivotRowData.value.some(item => item.code === value);

  if (isDuplicate) {
    // 중복이면 에러 메시지 반환 -> 입력창 아래 빨간색으로 표시됨
    callback(new Error('이미 존재하는 Code Key입니다. 다른 키를 입력해주세요.'));
  } else {
    // 통과
    callback();
  }
};

// Validation Rules
const rules = {
  code: [
    // required 체크 + 커스텀 중복 체크 (trigger: blur, change 둘 다 걸어서 실시간 반응)
    { required: true, validator: validateCodeUnique, trigger: ['blur', 'change'] }
  ],
  messageKo: [
    { required: true, message: '한국어 메시지를 입력해주세요.', trigger: 'blur' }
  ]
};

// AG Grid Locale
const localeText = reactive({
  filterOoo: '필터...',
  applyFilter: '필터 적용',
  resetFilter: '필터 초기화',
  pageSizeSelectorLabel: '페이지 크기:',
  page: '페이지',
  of: '/',
  to: '-',
  rowCount: '개',
  noRowsToShow: '표시할 데이터가 없습니다',
});

// --- Column Definitions
const colDefs = ref([
  {
    headerName: 'Code Key',
    field: 'code',
    width: 240, // 라벨 형태라 약간 여유 있게
    pinned: 'left',
    // 세로 중앙 정렬
    cellStyle: { display: 'flex', alignItems: 'center' },
    cellRenderer: (params) => {
      if (!params.value) return '';

      // Code Key 스타일: 회색 배경 + 진한 글씨 + 모노스페이스 폰트
      const codeStyle = `
        background-color: var(--el-border-color);
        border: 1px solid #dcdfe6;
        color: var(--el-text-color);
        padding: 2px 8px;
        border-radius: 4px;
        font-family: Consolas, Monaco, monospace;
        font-size: 11px;
        font-weight: 700;
        letter-spacing: -0.3px;
        display: inline-block;
        line-height: normal;
      `;
      return `<span style="${codeStyle}">${params.value}</span>`;
    }
  },
  {
    headerName: '한국어 (KO)',
    field: 'messageKo',
    flex: 1,
    // 세로 중앙 정렬을 위해 display: flex 추가
    cellStyle: { whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', display: 'flex', alignItems: 'center', fontSize: '13px' },
    cellRenderer: (params) => {
      if (!params.value) return `<span style="color: #ccc; font-size: 11px;">(미등록)</span>`;

      // KO 라벨 스타일 (초록색 계열)
      const badgeStyle = `
        color: #67c23a;
        background: rgba(103, 194, 58, 0.1);
        border: 1px solid rgba(103, 194, 58, 0.3);
        padding: 1px 5px;
        border-radius: 4px;
        font-size: 10px;
        font-weight: 700;
        margin-right: 6px;
        display: inline-block;
        line-height: normal;
      `;
      return `<span style="${badgeStyle}">KO</span><span>${params.value}</span>`;
    }
  },
  {
    headerName: 'English (EN)',
    field: 'messageEn',
    flex: 1,
    cellStyle: { whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', display: 'flex', alignItems: 'center', fontSize: '13px' },
    cellRenderer: (params) => {
      if (!params.value) return `<span style="color: #ccc; font-size: 11px;">(Unregistered)</span>`;

      // EN 라벨 스타일 (파란색 계열)
      const badgeStyle = `
        color: #409eff;
        background: rgba(64, 158, 255, 0.1);
        border: 1px solid rgba(64, 158, 255, 0.3);
        padding: 1px 5px;
        border-radius: 4px;
        font-size: 10px;
        font-weight: 700;
        margin-right: 6px;
        display: inline-block;
        line-height: normal;
      `;
      return `<span style="${badgeStyle}">EN</span><span>${params.value}</span>`;
    }
  },
  {
    headerName: '관리',
    width: 68,
    pinned: 'right',
    sortable: false,
    filter: false,
    cellStyle: { textAlign: 'center', display: 'flex', alignItems: 'center', justifyContent: 'center' },
    cellRenderer: () => {
      return `
        <span class="action-btn edit-btn" style="margin-right: 6px; color: var(--el-color-primary); font-weight:600; font-size: 11px; cursor: pointer;">
          <i class="el-icon-edit"></i> 수정
        </span>
        <span class="action-btn del-btn" style="color: var(--el-color-danger); font-weight:600; font-size: 11px; cursor: pointer;">
          <i class="el-icon-delete"></i> 삭제
        </span>
      `;
    }
  }
]);

// --- Methods ---
const onGridReady = (params) => { gridApi.value = params.api; };

// --- [Logic Add] Prefix Extraction ---
const extractPrefixes = (dataList) => {
  const prefixSet = new Set();
  dataList.forEach(item => {
    // 점(.)이 있는 경우 그룹으로 간주
    const lastDotIndex = item.code.lastIndexOf('.');
    if (lastDotIndex > -1) {
      // "login.title" -> "login." 추출
      const prefix = item.code.substring(0, lastDotIndex + 1);
      prefixSet.add(prefix);
    }
  });
  // Autocomplete용 포맷: [{ value: 'login.' }, ...]
  existingPrefixes.value = Array.from(prefixSet)
      .sort()
      .map(p => ({ value: p }));
};

// Autocomplete Filter Method
const querySearchGroup = (queryString, cb) => {
  const results = queryString
      ? existingPrefixes.value.filter(item => item.value.toLowerCase().includes(queryString.toLowerCase()))
      : existingPrefixes.value;
  cb(results);
};

// 데이터 조회 및 피벗 처리
const fetchData = async () => {
  const loadingInstance = ElLoading.service({ target: '.grid-container', text: '데이터 로딩 중...' });

  try {
    const response = await Api.post(ApiUrls.GET_MESSAGES_MANAGE, {
      locale: 'ALL',
      message: searchParams.keyword
    });
    const flatList = response.data || [];

    const groupedMap = new Map();
    flatList.forEach(item => {
      if (!groupedMap.has(item.code)) {
        groupedMap.set(item.code, {
          code: item.code,
          messageKo: '',
          messageEn: '',
          idKo: null,
          idEn: null,
          updatedAt: item.updatedAt
        });
      }
      const group = groupedMap.get(item.code);
      if (item.locale === 'ko') {
        group.messageKo = item.message;
        group.idKo = item.id;
      } else if (item.locale === 'en') {
        group.messageEn = item.message;
        group.idEn = item.id;
      }
    });

    const finalData = Array.from(groupedMap.values());
    pivotRowData.value = finalData;

    // [Logic Add] 데이터 로드 후 그룹(Prefix) 추출 갱신
    extractPrefixes(finalData);

  } catch (e) {
    console.error(e);
    ElMessage.error('데이터 조회 중 오류가 발생했습니다.');
  } finally {
    loadingInstance.close();
  }
};

const onSearch = () => fetchData();

const onReset = () => {
  searchParams.keyword = '';
  fetchData();
};

// --- Dialog Logic (수정됨: 세트 처리) ---
const openDialog = (row) => {
  if (row) {
    // 수정 모드: Row 데이터(Pivot)를 폼에 매핑
    isEditMode.value = true;
    formData.code = row.code;
    formData.messageKo = row.messageKo;
    formData.messageEn = row.messageEn;
    formData.idKo = row.idKo;
    formData.idEn = row.idEn;
  } else {
    // 등록 모드: 초기화
    isEditMode.value = false;
    formData.code = '';
    formData.messageKo = '';
    formData.messageEn = '';
    formData.idKo = null;
    formData.idEn = null;
  }
  dialogVisible.value = true;
};

// 저장 (KO/EN 일괄 처리)
const saveData = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      const loading = ElLoading.service({ text: '저장 중...' });
      try {
        // [백엔드 연동 포인트]
        // 실제로는 List<MessageRequestDto> 형태로 한번에 보내거나,
        // 아래처럼 순차 호출을 해야 합니다.

        // 1. KO 저장
        const reqKo = {
          id: formData.idKo, // null이면 Insert
          code: formData.code,
          locale: 'ko',
          message: formData.messageKo
        };
        // await Api.post(ApiUrls.SAVE_MESSAGE, reqKo); // 실제 호출

        // 2. EN 저장
        const reqEn = {
          id: formData.idEn,
          code: formData.code,
          locale: 'en',
          message: formData.messageEn
        };
        // await Api.post(ApiUrls.SAVE_MESSAGE, reqEn); // 실제 호출

        // Mock Simulation
        console.log('KO Save:', reqKo);
        console.log('EN Save:', reqEn);
        await new Promise(r => setTimeout(r, 500));

        ElMessage.success('성공적으로 저장되었습니다.');
        dialogVisible.value = false;
        fetchData(); // 그리드 갱신

      } catch (e) {
        console.error(e);
        ElMessage.error('저장에 실패했습니다.');
      } finally {
        loading.close();
      }
    }
  });
};

// 삭제 (Code 기준 일괄 삭제)
const handleDelete = async (row) => {

  await Dialogs.customConfirm(
      '삭제 확인',
      `코드 [${row.code}]의 한글/영어 리소스를 모두 삭제하시겠습니까?`,
      '삭제',
      '취소',
      '485px',
      'warning'
  );

  // ElMessageBox.confirm(
  //     `코드 [${row.code}]의 한글/영어 리소스를 모두 삭제하시겠습니까?`,
  //     '삭제 확인',
  //     { confirmButtonText: '삭제', cancelButtonText: '취소', type: 'warning' }
  // ).then(async () => {
  //   try {
  //     // [백엔드 연동 포인트] Code로 삭제하는 API 호출
  //     // await Api.post(ApiUrls.DELETE_MESSAGE_BY_CODE, { code: row.code });
  //
  //     console.log('Delete Code:', row.code); // Mock
  //     ElMessage.success('삭제되었습니다.');
  //     fetchData();
  //   } catch (e) {
  //     ElMessage.error('삭제 실패');
  //   }
  // });
};

// --- Grid Cell Click Event ---
const onCellClicked = (params) => {
  const target = params.event.target;
  if (target.closest('.edit-btn')) {
    openDialog(params.data);
  } else if (target.closest('.del-btn')) {
    handleDelete(params.data);
  }
};

onMounted(() => {
  fetchData();
});
</script>

<style>
@import "ag-grid-community/styles/ag-grid.css";
@import "ag-grid-community/styles/ag-theme-alpine.css";
</style>

<style scoped>
/* 기존 스타일 유지 */
.i18n-management-container {
  display: flex;
  flex-direction: column;
  height: 620px;
  width: 820px;
  background-color: var(--el-bg-color);
  font-size: 12px;
  margin: 4px 0 0 0;
  box-sizing: border-box;

}

.top-search-bar {
  height: 48px; /* 높이를 컴팩트하게 고정 */
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  margin: 0 0 4px 0;
}

/* 왼쪽 영역 */
.section-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 140px;
}
.page-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}
.count-tag {
  font-size: 11px;
  font-weight: 600;
  border: none;
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
}

/* 중앙 영역 (검색창) */
.section-center {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 0 16px;
  margin: 0 0 0 20px;
}
/* 검색창 너비 조정 */
.compact-input {
  width: 100%;
  max-width: 320px;
}

/* 오른쪽 영역 (버튼) */
.section-right {
  display: flex;
  align-items: center;
  gap: 0px; /* 버튼그룹을 쓰므로 갭 제거 */
}

.grid-container {
  flex-grow: 1;
  width: 100%;
}

:deep(.el-input), :deep(.el-select) {
  vertical-align: middle;
}

/* 다이얼로그 구분선 폰트 */
:deep(.el-divider__text) {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* Pagination Style */
:deep(.ag-paging-panel) {
  height: 36px !important;
  padding: 0 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--el-border-color-light);
}
:deep(.ag-paging-row-summary-panel),
:deep(.ag-paging-page-summary-panel) {
  display: flex;
  align-items: center;
  gap: 8px;
}
:deep(.action-btn:hover) {
  text-decoration: underline;
}
</style>

<style>
/* AG Grid Dark Mode (Global) */
html.dark .ag-theme-alpine {
  --ag-background-color: var(--el-bg-color-overlay);
  --ag-data-color: var(--el-text-color-primary);
  --ag-foreground-color: var(--el-text-color-primary);
  --ag-header-background-color: var(--el-bg-color);
  --ag-header-foreground-color: var(--el-text-color-primary);
  --ag-row-background-color: var(--el-bg-color-overlay);
  --ag-odd-row-background-color: #1d1e22;
  --ag-border-color: var(--el-border-color);
  --ag-input-background-color: var(--el-fill-color-blank);
  --ag-input-border-color: var(--el-border-color);
  --ag-input-focus-border-color: var(--el-color-primary);
}
</style>