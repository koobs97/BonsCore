<template>
  <div class="menu-management-container">

    <!-- 1. 검색 패널 -->
    <div class="top-search-bar">
      <!-- 왼쪽: 타이틀 & 통계 -->
      <div class="section-left">
        <span class="page-title">메뉴 관리</span>
        <el-tag type="info" size="small" effect="plain" round class="count-tag">
          Total {{ rowData.length }}
        </el-tag>
      </div>

      <!-- 중앙: 검색창 -->
      <div class="section-center">
        <el-input
            v-model="searchParams.keyword"
            placeholder="메뉴명 또는 메뉴ID 검색"
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
          :rowData="rowData"
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

    <!-- 3. 등록/수정 다이얼로그 -->
    <el-dialog
        v-model="dialogVisible"
        :title="isEditMode ? '메뉴 정보 수정' : '새 메뉴 등록'"
        width="600px"
        align-center
        destroy-on-close
        :close-on-click-modal="false"
    >
      <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-width="120px"
          size="default"
          status-icon
      >
        <!-- 메뉴 ID -->
        <el-form-item label="메뉴 ID" prop="menuId">
          <el-input
              v-model="formData.menuId"
              :disabled="isEditMode"
              placeholder="ex) SC00000001"
              maxlength="10"
              show-word-limit
          >
            <template #prefix><el-icon><Key /></el-icon></template>
          </el-input>
          <span v-if="!isEditMode" style="font-size: 11px; color: var(--el-text-color-secondary); margin-top: 4px;">
            * 고유한 메뉴 ID를 입력하세요. (등록 후 수정 불가)
          </span>
        </el-form-item>

        <!-- 상위 메뉴 선택 (Tree Select) -->
        <el-form-item label="상위 메뉴" prop="parentMenuId">
          <el-tree-select
              v-model="formData.parentMenuId"
              :data="menuTreeData"
              check-strictly
              :render-after-expand="false"
              placeholder="상위 메뉴 선택 (없으면 최상위)"
              clearable
              style="width: 100%"
          >
            <template #default="{ data: { label, value } }">
              <span>{{ label }}</span>
              <span style="font-size: 11px; color: #999; margin-left: 8px;">({{ value }})</span>
            </template>
          </el-tree-select>
        </el-form-item>

        <el-divider content-position="left">메뉴 상세 정보</el-divider>

        <!-- 메뉴 이름 -->
        <el-form-item label="메뉴 이름" prop="menuName">
          <el-input
              v-model="formData.menuName"
              placeholder="메뉴 이름을 입력하세요"
          />
        </el-form-item>

        <!-- 메뉴 URL -->
        <el-form-item label="메뉴 URL" prop="menuUrl">
          <el-input
              v-model="formData.menuUrl"
              placeholder="이동할 경로 (ex: /system/user)"
          >
            <template #prefix><el-icon><Link /></el-icon></template>
          </el-input>
        </el-form-item>

        <div style="display: flex; gap: 20px;">
          <!-- 정렬 순서 -->
          <el-form-item label="정렬 순서" prop="sortOrder" style="flex: 1;">
            <el-input-number
                v-model="formData.sortOrder"
                :min="0"
                controls-position="right"
                style="width: 100%;"
            />
          </el-form-item>

          <!-- 화면 표시 여부 -->
          <el-form-item label="화면 표시" prop="isVisible" style="flex: 1;">
            <el-switch
                v-model="formData.isVisible"
                active-value="Y"
                inactive-value="N"
                active-text="표시"
                inactive-text="숨김"
                inline-prompt
                style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
            />
          </el-form-item>
        </div>

      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">취소</el-button>
          <el-button type="primary" @click="saveData">
            {{ isEditMode ? '저장' : '등록' }}
          </el-button>
        </span>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { AgGridVue } from 'ag-grid-vue3';
import { ElLoading, ElMessage, ElMessageBox } from 'element-plus';
import { Search, Refresh, Plus, Key, Link } from '@element-plus/icons-vue';

// --- State Variables ---
const gridApi = ref(null);
const searchParams = reactive({ keyword: '' });
const rowData = ref([]); // Grid Row Data
const defaultColDef = { resizable: true, sortable: true, filter: true };

// Dialog State
const dialogVisible = ref(false);
const isEditMode = ref(false);
const formRef = ref(null);

// Form Data (DB 컬럼과 매핑)
const formData = reactive({
  menuId: '',
  menuName: '',
  menuUrl: '',
  parentMenuId: '',
  sortOrder: 0,
  isVisible: 'Y'
});

// Mock Data (SQL INSERT 문 기반)
const mockDbData = [
  { menuId: 'SC10000000', menuName: '서비스', menuUrl: '', parentMenuId: '', sortOrder: 1, isVisible: 'N' },
  { menuId: 'SC00000000', menuName: '시스템 관리', menuUrl: '', parentMenuId: '', sortOrder: 1, isVisible: 'N' },
  { menuId: 'SC00000001', menuName: '사용자 관리', menuUrl: 'User', parentMenuId: 'SC00000000', sortOrder: 2, isVisible: 'Y' },
  { menuId: 'SC00000002', menuName: '메뉴 관리', menuUrl: 'Menu', parentMenuId: 'SC00000000', sortOrder: 3, isVisible: 'Y' },
  { menuId: 'SC00000003', menuName: '활동 로그', menuUrl: 'Log', parentMenuId: 'SC00000000', sortOrder: 4, isVisible: 'Y' },
  { menuId: 'SC00000004', menuName: '메시지 관리', menuUrl: 'Message', parentMenuId: 'SC00000000', sortOrder: 5, isVisible: 'Y' },
  { menuId: 'SC10000001', menuName: '시간대별 예측', menuUrl: 'WaitingAnalysisCard', parentMenuId: 'SC10000000', sortOrder: 5, isVisible: 'Y' },
  { menuId: 'SC10000002', menuName: '저장소', menuUrl: 'StoreArchive', parentMenuId: 'SC10000000', sortOrder: 6, isVisible: 'Y' },
];

// --- Computed: Tree Data for Select ---
// 상위 메뉴 선택 시 계층 구조로 보여주기 위함
const menuTreeData = computed(() => {
  const buildTree = (items, parentId = '') => {
    return items
        .filter(item => (item.parentMenuId || '') === parentId)
        .map(item => ({
          value: item.menuId,
          label: item.menuName,
          children: buildTree(items, item.menuId)
        }));
  };
  return buildTree(rowData.value);
});

// --- Validation ---
const validateIdUnique = (rule, value, callback) => {
  if (!value) return callback(new Error('메뉴 ID를 입력해주세요.'));
  if (isEditMode.value) return callback();

  const isDuplicate = rowData.value.some(item => item.menuId === value);
  if (isDuplicate) callback(new Error('이미 존재하는 메뉴 ID입니다.'));
  else callback();
};

const rules = {
  menuId: [{ required: true, validator: validateIdUnique, trigger: 'blur' }],
  menuName: [{ required: true, message: '메뉴 이름을 입력해주세요.', trigger: 'blur' }],
  // URL은 필수 아님 (상위 폴더일 수 있음)
};

// --- AG Grid Settings ---
const localeText = {
  filterOoo: '필터...',
  applyFilter: '적용',
  resetFilter: '초기화',
  noRowsToShow: '조회된 메뉴가 없습니다',
};

// ID -> Name 매핑을 위한 Helper
const getMenuNameById = (id) => {
  const found = rowData.value.find(d => d.menuId === id);
  return found ? found.menuName : id;
};

// Column Definitions
const colDefs = ref([
  {
    headerName: 'ID',
    field: 'menuId',
    width: 120,
    pinned: 'left',
    cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'center' },
    cellRenderer: (params) => {
      // ID 스타일
      return `<span style="font-family: monospace; font-weight: bold; color: var(--el-text-color-primary);">${params.value}</span>`;
    }
  },
  {
    headerName: '메뉴 이름',
    field: 'menuName',
    flex: 1,
    cellStyle: { display: 'flex', alignItems: 'center' },
    tooltipField: 'menuName'
  },
  {
    headerName: 'URL',
    field: 'menuUrl',
    width: 170,
    cellStyle: { display: 'flex', alignItems: 'center' },
    cellRenderer: (params) => {
      if (!params.value) return '<span style="color:#ccc;">-</span>';
      return `<span style="color: var(--el-color-primary);">${params.value}</span>`;
    }
  },
  {
    headerName: '상위 메뉴',
    field: 'parentMenuId',
    width: 120,
    cellStyle: { display: 'flex', alignItems: 'center' },
    cellRenderer: (params) => {
      if (!params.value) return '<span style="color:#ccc; font-size:11px;">(최상위)</span>';
      const pName = getMenuNameById(params.value);
      return `
        <div style="display:flex; flex-direction:column; line-height: 1.2;">
           <span>${pName}</span>
           <span style="font-size:10px; color:#999;">${params.value}</span>
        </div>
      `;
    }
  },
  {
    headerName: '순서',
    field: 'sortOrder',
    width: 80,
    cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'center' }
  },
  {
    headerName: '표시',
    field: 'isVisible',
    width: 80,
    cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'center' },
    cellRenderer: (params) => {
      const isY = params.value === 'Y';
      const color = isY ? '#67c23a' : '#909399';
      const bg = isY ? 'rgba(103, 194, 58, 0.1)' : 'rgba(144, 147, 153, 0.1)';
      const text = isY ? 'Y' : 'N';
      return `<span style="background:${bg}; color:${color}; padding:2px 8px; border-radius:10px; font-weight:bold; font-size:11px;">${text}</span>`;
    }
  },
  {
    headerName: '관리',
    width: 80,
    pinned: 'right',
    sortable: false,
    filter: false,
    cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'center' },
    cellRenderer: () => {
      return `
        <div style="display: flex; gap: 10px;">
            <i class="action-btn edit-btn el-icon-edit" style="color: var(--el-color-primary); cursor: pointer; font-size: 14px;" title="수정">✎</i>
            <i class="action-btn del-btn el-icon-delete" style="color: var(--el-color-danger); cursor: pointer; font-size: 14px;" title="삭제">✕</i>
        </div>
      `;
    }
  }
]);

// --- Methods ---
const onGridReady = (params) => { gridApi.value = params.api; };

// 조회 (Mock Data Fetch)
const fetchData = async () => {
  const loading = ElLoading.service({ target: '.grid-container', text: '데이터 로딩 중...' });
  try {
    // 실제 API 호출 시: const res = await Api.get('/api/menu', { params: searchParams });
    await new Promise(r => setTimeout(r, 300)); // Latency Simulation

    // 검색 필터링 (프론트엔드 모의 구현)
    let filtered = [...mockDbData];
    if (searchParams.keyword) {
      const k = searchParams.keyword.toLowerCase();
      filtered = filtered.filter(item =>
          item.menuName.toLowerCase().includes(k) ||
          item.menuId.toLowerCase().includes(k)
      );
    }
    // 정렬 (상위 메뉴 -> 순서)
    filtered.sort((a, b) => {
      if (a.parentMenuId === b.parentMenuId) return a.sortOrder - b.sortOrder;
      return (a.parentMenuId || '').localeCompare(b.parentMenuId || '');
    });

    rowData.value = filtered;
  } finally {
    loading.close();
  }
};

const onSearch = () => fetchData();
const onReset = () => {
  searchParams.keyword = '';
  fetchData();
};

// 다이얼로그 열기
const openDialog = (row) => {
  if (formRef.value) formRef.value.resetFields();

  if (row) {
    isEditMode.value = true;
    // 값 복사
    Object.assign(formData, row);
  } else {
    isEditMode.value = false;
    // 초기값
    Object.assign(formData, {
      menuId: '', menuName: '', menuUrl: '',
      parentMenuId: '', sortOrder: 0, isVisible: 'Y'
    });
  }
  dialogVisible.value = true;
};

// 저장 로직
const saveData = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      const loading = ElLoading.service({ text: '저장 중...' });
      try {
        // [백엔드 연동 포인트] API 호출
        // await Api.post(isEditMode.value ? '/api/menu/update' : '/api/menu/create', formData);

        await new Promise(r => setTimeout(r, 500)); // Mock API time

        // Mock 데이터 갱신
        if (isEditMode.value) {
          const idx = mockDbData.findIndex(d => d.menuId === formData.menuId);
          if (idx > -1) mockDbData[idx] = { ...formData };
        } else {
          mockDbData.push({ ...formData });
        }

        ElMessage.success('저장되었습니다.');
        dialogVisible.value = false;
        fetchData();
      } catch (e) {
        ElMessage.error('오류가 발생했습니다.');
      } finally {
        loading.close();
      }
    }
  });
};

// 삭제 로직
const handleDelete = async (row) => {
  // 하위 메뉴가 있는지 체크 (Mock)
  const hasChildren = rowData.value.some(r => r.parentMenuId === row.menuId);
  if (hasChildren) {
    ElMessage.warning('하위 메뉴가 존재하여 삭제할 수 없습니다.');
    return;
  }

  try {
    await ElMessageBox.confirm(
        `메뉴 [${row.menuName}]을(를) 정말 삭제하시겠습니까?`,
        '삭제 확인',
        { confirmButtonText: '삭제', cancelButtonText: '취소', type: 'warning' }
    );

    // [백엔드 연동 포인트] await Api.delete(`/api/menu/${row.menuId}`);
    const idx = mockDbData.findIndex(d => d.menuId === row.menuId);
    if (idx > -1) mockDbData.splice(idx, 1);

    ElMessage.success('삭제되었습니다.');
    fetchData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('삭제 실패');
  }
};

const onCellClicked = (params) => {
  const target = params.event.target;
  if (target.closest('.edit-btn')) openDialog(params.data);
  else if (target.closest('.del-btn')) handleDelete(params.data);
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
/* 메인 컨테이너 (기존 스타일 유지하되 클래스명 변경) */
.menu-management-container {
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
  height: 48px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  margin: 0 0 4px 0;
}

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

.section-center {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 0 16px;
  margin: 0 0 0 20px;
}
.compact-input {
  width: 100%;
  max-width: 320px;
}

.section-right {
  display: flex;
  align-items: center;
}

.grid-container {
  flex-grow: 1;
  width: 100%;
}

:deep(.el-input), :deep(.el-select), :deep(.el-tree-select) {
  vertical-align: middle;
}

/* Pagination Style Override */
:deep(.ag-paging-panel) {
  height: 36px !important;
  padding: 0 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--el-border-color-light);
}
:deep(.action-btn:hover) {
  text-decoration: underline;
}
</style>

<style>
/* Dark Mode Support (Global) */
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
}
</style>