<script setup>
/**
 * ========================================
 * 파일명   : Message.vue
 * ----------------------------------------
 * 설명     : 메시지 관리
 * 작성자   : koobonsang
 * 버전     : 1.0
 * 작성일자 : 2025-12-11
 * ========================================
 */
import { ref, reactive, onMounted, computed } from 'vue';
import { AgGridVue } from 'ag-grid-vue3';
import { ElMessage } from 'element-plus';
import { Search, Refresh, Plus, Key } from '@element-plus/icons-vue';
import { ApiUrls } from "@/api/apiUrls.js";
import { Api } from "@/api/axiosInstance.js";
import { Dialogs } from "@/common/dialogs.js";
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

// --- State Variables ---
const gridApi = ref(null);
const searchParams = reactive({ keyword: '' });
const pivotRowData = ref([]); // 피벗(그룹핑)된 데이터
const existingPrefixes = ref([]); // 추출된 코드 그룹(Prefix) 목록
const defaultColDef = { resizable: true, sortable: true, filter: true };

// Dialog State
const dialogVisible = ref(false);
const isEditMode = ref(false);
const formRef = ref(null);

// Form 데이터 구조: code 하나에 ko/en 메시지 포함
const formData = reactive({
  code: '',
  messageKo: '',
  messageEn: '',
  idKo: null,
  idEn: null
});

// Custom Validator: 중복 체크
const validateCodeUnique = (rule, value, callback) => {
  if (!value) {
    return callback(new Error(t('message.manage.valid.req_code')));
  }

  // 수정 모드일 때는 중복 체크 건너뜀
  if (isEditMode.value) {
    return callback();
  }

  // pivotRowData에는 현재 화면에 로드된 모든 고유 Code들이 있음
  // some()을 사용하여 입력된 value와 정확히 일치하는 Code가 있는지 확인
  const isDuplicate = pivotRowData.value.some(item => item.code === value);

  if (isDuplicate) {
    // 중복이면 에러 메시지 반환 -> 입력창 아래 빨간색으로 표시됨
    callback(new Error(t('message.manage.valid.dup_code')));
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
    { required: true, message: () => t('message.manage.valid.req_ko'), trigger: 'blur' }
  ]
};

// AG Grid Locale
const localeText = computed(() => ({
  // Paging
  page: t('grid.locale.page'),
  to: t('grid.locale.to'),
  of: t('grid.locale.of'),
  next: t('grid.locale.next'),
  last: t('grid.locale.last'),
  first: t('grid.locale.first'),
  previous: t('grid.locale.previous'),
  loadingOoo: t('message.manage.alert.loading_data'), // 로딩 텍스트 재사용

  // Empty & Error
  noRowsToShow: t('grid.locale.noRowsToShow'),

  // Filter
  filterOoo: t('grid.locale.filterOoo'),
  applyFilter: t('grid.locale.applyFilter'),
  resetFilter: t('grid.locale.resetFilter'),
  clearFilter: t('grid.locale.clearFilter'),
  equals: t('grid.locale.equals'),
  notEqual: t('grid.locale.notEqual'),
  contains: t('grid.locale.contains'),
  notContains: t('grid.locale.notContains'),
  startsWith: t('grid.locale.startsWith'),
  endsWith: t('grid.locale.endsWith'),
}));

const formatDisplayText = (text) => {
  if (!text) return '';
  // 1. <br> 태그를 공백으로 변환
  // 2. 실제 엔터값(\n)을 공백으로 변환
  // 3. 연속된 공백을 하나로 합침
  return text
      .replace(/<br\s*\/?>/gi, ' ')
      .replace(/\n/g, ' ')
      .replace(/\s+/g, ' ');
};

// --- Column Definitions
const colDefs = ref([
  {
    headerName: t('message.manage.grid.code'),
    field: 'code',
    width: 240,
    pinned: 'left',
    tooltipField: 'code', // 마우스 올리면 전체 키 보임
    cellStyle: { display: 'flex', alignItems: 'center' },
    cellRenderer: (params) => {
      if (!params.value) return '';
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
        line-height: normal;
      `;
      return `<span style="${codeStyle}">${params.value}</span>`;
    }
  },
  {
    headerName: t('message.manage.grid.ko'),
    field: 'messageKo',
    flex: 1,
    // 툴팁 활성화: 마우스 오버 시 원본(줄바꿈 포함) 내용 표시
    tooltipValueGetter: (params) => params.value,
    // flex 레이아웃으로 뱃지와 텍스트 정렬
    cellStyle: { display: 'flex', alignItems: 'center', paddingRight: '10px' },
    cellRenderer: (params) => {
      if (!params.value) return `<span style="color: #ccc; font-size: 11px;">(${t('message.manage.grid.unregistered')})</span>`;

      // 1. 화면 표시용 텍스트 정제 (HTML 제거, 한 줄 만들기)
      const displayText = formatDisplayText(params.value);

      const badgeStyle = `
        color: #67c23a;
        background: rgba(103, 194, 58, 0.1);
        border: 1px solid rgba(103, 194, 58, 0.3);
        padding: 1px 5px;
        border-radius: 4px;
        font-size: 10px;
        font-weight: 700;
        margin-right: 8px;
        flex-shrink: 0; /* 뱃지 크기 고정 */
        line-height: normal;
      `;

      // 2. 텍스트 스타일: 말줄임표(...) 처리
      const textStyle = `
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 13px;
        color: var(--el-text-color-regular);
      `;

      return `
        <span style="${badgeStyle}">KO</span>
        <span style="${textStyle}">${displayText}</span>
      `;
    }
  },
  {
    headerName: t('message.manage.grid.en'),
    field: 'messageEn',
    flex: 1,
    tooltipValueGetter: (params) => params.value, // 원본 내용 툴팁
    cellStyle: { display: 'flex', alignItems: 'center', paddingRight: '10px' },
    cellRenderer: (params) => {
      if (!params.value) return `<span style="color: #ccc; font-size: 11px;">(${t('message.manage.grid.unregistered')})</span>`;

      const displayText = formatDisplayText(params.value);

      const badgeStyle = `
        color: #409eff;
        background: rgba(64, 158, 255, 0.1);
        border: 1px solid rgba(64, 158, 255, 0.3);
        padding: 1px 5px;
        border-radius: 4px;
        font-size: 10px;
        font-weight: 700;
        margin-right: 8px;
        flex-shrink: 0;
        line-height: normal;
      `;

      const textStyle = `
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 13px;
        color: var(--el-text-color-regular);
      `;

      return `
        <span style="${badgeStyle}">EN</span>
        <span style="${textStyle}">${displayText}</span>
      `;
    }
  },
  {
    headerName: t('message.manage.grid.action'),
    width: 68,
    pinned: 'right',
    sortable: false,
    filter: false,
    cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'center' },
    cellRenderer: () => {
      // 버튼 아이콘만 깔끔하게 표시하거나, 텍스트를 유지하되 스타일 통일
      return `
        <div style="display: flex; gap: 8px;">
            <i class="action-btn edit-btn el-icon-edit" style="color: var(--el-color-primary); cursor: pointer; font-size: 14px;" title="수정">✎</i>
            <i class="action-btn del-btn el-icon-delete" style="color: var(--el-color-danger); cursor: pointer; font-size: 14px;" title="삭제">✕</i>
        </div>
      `;
    }
  }
]);

// --- Methods ---
const onGridReady = (params) => { gridApi.value = params.api; };

// --- Prefix Extraction ---
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
  const response = await Api.get(ApiUrls.GET_MESSAGES_MANAGE, {
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
};

/**
 * 조회
 * @returns {Promise<void>}
 */
const onSearch = () => fetchData();

/**
 * 초기화
 */
const onReset = () => {
  searchParams.keyword = '';
  fetchData();
};

/**
 * 다이얼로그
 * @param row
 */
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

/**
 * 저장 (KO/EN 일괄 처리)
 * @returns {Promise<void>}
 */
const saveData = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      await Dialogs.customConfirm(
          t('message.manage.alert.save_title'),
          t('message.manage.alert.save_confirm'),
          t('message.manage.btn.save'),
          t('message.manage.btn.cancel'),
          '485px'
      );

      const requestList = [
        {
          id: formData.idKo, // null이면 신규
          code: formData.code,
          locale: 'ko',
          message: formData.messageKo
        },
        {
          id: formData.idEn, // null이면 신규
          code: formData.code,
          locale: 'en',
          message: formData.messageEn
        }
      ];

      await Api.put(ApiUrls.SAVE_MESSAGES, requestList);
      ElMessage.success(t('message.manage.alert.save_success'));
      dialogVisible.value = false;
      await fetchData();
    }
  });
};

/**
 * 삭제 (Code 기준 일괄 삭제)
 * @param row
 * @returns {Promise<void>}
 */
const handleDelete = async (row) => {

  await Dialogs.customConfirm(
      t('message.manage.alert.del_title'),
      t('message.manage.alert.del_confirm', { code: row.code }),
      t('message.manage.btn.delete'),
      t('message.manage.btn.cancel'),
      '485px',
      'warning'
  );

  await Api.delete(`${ApiUrls.DELETE_MESSAGE}/${encodeURIComponent(row.code)}`);
  ElMessage.success(t('message.manage.alert.del_success'));
  await fetchData();

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
<template>
  <div class="i18n-management-container">

    <!-- 1. 검색 패널 -->
    <div class="top-search-bar">
      <!-- 왼쪽: 타이틀 & 통계 -->
      <div class="section-left">
        <span class="page-title">{{ t('message.manage.title') }}</span>
        <el-tag type="info" size="small" effect="plain" round class="count-tag">
          Total {{ pivotRowData.length }}
        </el-tag>
      </div>

      <!-- 중앙: 검색창 (넓게 배치) -->
      <div class="section-center">
        <el-input
            v-model="searchParams.keyword"
            :placeholder="t('message.manage.placeholder_search')"
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
          <el-button plain size="small" :icon="Search" @click="onSearch">{{ t('message.manage.btn.search') }}</el-button>
          <el-button size="small" :icon="Refresh" @click="onReset" :title="t('message.manage.btn.reset')"></el-button>
        </el-button-group>
        <el-divider direction="vertical" style="height: 18px; margin: 0 8px;" />
        <el-button type="primary" size="small" :icon="Plus" @click="openDialog(null)">{{ t('message.manage.btn.add') }}</el-button>
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
          :paginationPageSizeSelector="[30, 50, 100]"
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
        :title="isEditMode ? t('message.manage.dialog.title_edit') : t('message.manage.dialog.title_add')"
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
              :placeholder="t('message.manage.dialog.placeholder_code')"
              class="w-full"
              style="width: 100%"
              trigger-on-focus
              clearable
          >
            <template #prefix><el-icon><Key /></el-icon></template>
            <template #default="{ item }">
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-weight: bold; color: var(--el-text-color-primary);">{{ item.value }}</span>
                <span style="font-size: 10px; color: #999; margin-left: 10px;">{{ t('message.manage.dialog.existing_group') }}</span>
              </div>
            </template>
          </el-autocomplete>

          <span v-if="isEditMode" style="font-size: 11px; color: var(--el-color-warning); margin-top: 4px;">
            {{ t('message.manage.dialog.warn_code_fixed') }}
          </span>
          <span v-else style="font-size: 11px; color: var(--el-text-color-secondary); margin-top: 4px;">
            {{ t('message.manage.dialog.info_prefix') }}
          </span>
        </el-form-item>

        <el-divider content-position="left">{{ t('message.manage.dialog.msg_input') }}</el-divider>

        <!-- Message KO -->
        <el-form-item :label="t('message.manage.grid.ko')" prop="messageKo">
          <el-input
              v-model="formData.messageKo"
              type="textarea"
              :rows="3"
              :placeholder="t('message.manage.dialog.input_ko')"
          />
        </el-form-item>

        <!-- Message EN -->
        <el-form-item :label="t('message.manage.grid.en')" prop="messageEn">
          <el-input
              v-model="formData.messageEn"
              type="textarea"
              :rows="3"
              :placeholder="t('message.manage.dialog.input_en')"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ t('message.manage.btn.cancel') }}</el-button>
          <el-button type="primary" @click="saveData">
            {{ isEditMode ? t('message.manage.dialog.save_all') : t('message.manage.dialog.add_batch') }}
          </el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

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