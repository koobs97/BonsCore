<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue';
import { AgGridVue } from 'ag-grid-vue3';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import { Search, Plus, Delete, Key, Check, User } from '@element-plus/icons-vue';

// --- MOCK DATA (DB 시뮬레이션) ---
const mockRoles = ref([
  { roleId: 'ADMIN', roleName: '관리자', description: '시스템 전체 관리' },
  { roleId: 'USER', roleName: '일반 사용자', description: '기본 조회 권한' },
]);

const mockMenus = [
  { menuId: 'SC00', menuName: '시스템', parentId: null },
  { menuId: 'SC01', menuName: '사용자관리', parentId: 'SC00' },
  { menuId: 'SC02', menuName: '메뉴관리', parentId: 'SC00' },
  { menuId: 'SC03', menuName: '권한관리', parentId: 'SC00' },
  { menuId: 'BZ00', menuName: '영업관리', parentId: null },
  { menuId: 'BZ01', menuName: '주문조회', parentId: 'BZ00' },
  { menuId: 'BZ02', menuName: '매출분석', parentId: 'BZ00' },
];

const mockUsers = [
  { userId: 'u1', userName: '홍길동' },
  { userId: 'u2', userName: '김철수' },
  { userId: 'u3', userName: '이영희' },
  { userId: 'u4', userName: '박민수' },
  { userId: 'u5', userName: '최지우' },
  { userId: 'u6', userName: '정우성' },
];

// 매핑 데이터
const mockRoleMenu = reactive({ 'ADMIN': ['SC00','SC01','SC02','SC03','BZ00','BZ01','BZ02'], 'USER': ['BZ00','BZ01'] });
const mockRoleUser = reactive({ 'ADMIN': ['u1'], 'USER': ['u2', 'u3'] });

// --- STATE ---
const gridApi = ref(null);
const searchKeyword = ref('');
const roleList = ref([]);
const selectedRole = ref(null);
const activeTab = ref('basic');
const isNewRole = ref(false);

// Form
const formBasic = reactive({ roleId: '', roleName: '', description: '' });

// Tree
const menuTreeRef = ref(null);
const menuFilterText = ref('');
const menuTreeData = computed(() => {
  const build = (pid) => mockMenus.filter(m => m.parentId === pid).map(m => ({ ...m, children: build(m.menuId) }));
  return build(null);
});

// Transfer
const allUsersSource = computed(() => mockUsers);
const assignedUserIds = ref([]);

// Grid Config (Compact)
const defaultColDef = { sortable: true, resizable: true };
const roleColDefs = [
  { headerName: 'ID', field: 'roleId', width: 90, cellStyle: { fontWeight: 'bold', color: '#666', textAlign: 'left' } },
  { headerName: '권한명', field: 'roleName', flex: 1, cellStyle: { textAlign: 'left' } },
];

// --- ACTIONS ---
const onGridReady = (params) => {
  gridApi.value = params.api;
  roleList.value = [...mockRoles.value];
};

const onSearchFilter = () => {
  if (!gridApi.value) return;
  gridApi.value.setQuickFilter(searchKeyword.value);
};

const onRoleSelect = (event) => {
  const row = event.data;
  // 변경사항 체크 로직 생략(바로 전환)
  selectedRole.value = { ...row };
  isNewRole.value = false;
  activeTab.value = 'basic';

  // 데이터 바인딩
  Object.assign(formBasic, row);

  // 메뉴 트리 체크
  setTimeout(() => {
    if (menuTreeRef.value) {
      menuTreeRef.value.setCheckedKeys(mockRoleMenu[row.roleId] || [], false);
    }
  }, 50);

  // 사용자 매핑
  assignedUserIds.value = [...(mockRoleUser[row.roleId] || [])];
};

const onClickAddRole = () => {
  if (gridApi.value) gridApi.value.deselectAll();
  const newRole = { roleId: '', roleName: '', description: '' };
  selectedRole.value = newRole;
  isNewRole.value = true;
  activeTab.value = 'basic';
  Object.assign(formBasic, newRole);
  assignedUserIds.value = [];
  if (menuTreeRef.value) menuTreeRef.value.setCheckedKeys([]);
};

const saveBasicInfo = async () => {
  if(!formBasic.roleId) return ElMessage.warning('권한 ID를 입력하세요.');
  const loading = ElLoading.service({ target: '.panel-right', text: '저장 중' });

  await new Promise(r => setTimeout(r, 300)); // Mock API

  if (isNewRole.value) {
    mockRoles.value.push({ ...formBasic });
    roleList.value = [...mockRoles.value]; // refresh grid
  } else {
    const idx = mockRoles.value.findIndex(r => r.roleId === formBasic.roleId);
    if(idx > -1) mockRoles.value[idx] = { ...formBasic };
    roleList.value = [...mockRoles.value];
    if(gridApi.value) gridApi.value.refreshCells();
  }

  ElMessage.success('저장되었습니다.');
  isNewRole.value = false;
  selectedRole.value = { ...formBasic };
  loading.close();
};

// 메뉴 필터링
watch(menuFilterText, (val) => menuTreeRef.value?.filter(val));
const filterMenuNode = (value, data) => !value || data.menuName.includes(value);

const saveMenuMapping = async () => {
  const rid = selectedRole.value.roleId;
  const keys = [...menuTreeRef.value.getCheckedKeys(), ...menuTreeRef.value.getHalfCheckedKeys()];
  mockRoleMenu[rid] = keys;
  ElMessage.success('메뉴 권한이 저장되었습니다.');
};

const saveUserMapping = async () => {
  const rid = selectedRole.value.roleId;
  mockRoleUser[rid] = assignedUserIds.value;
  ElMessage.success('사용자가 할당되었습니다.');
};

const onDeleteRole = async () => {
  await ElMessageBox.confirm('정말 삭제하시겠습니까?', '경고', { type: 'warning' });
  const rid = selectedRole.value.roleId;
  const idx = mockRoles.value.findIndex(r => r.roleId === rid);
  if (idx > -1) mockRoles.value.splice(idx, 1);
  roleList.value = [...mockRoles.value];
  selectedRole.value = null;
  ElMessage.success('삭제되었습니다.');
};

onMounted(() => {
  // onGridReady에서 데이터 로드
});
</script>

<template>
  <div class="role-management-container">

    <!-- [좌측 패널] 권한 목록 (Master) -->
    <div class="panel-left">
      <!-- 1. 좌측 헤더 (검색 & 추가) -->
      <div class="panel-header">
        <span class="panel-title">권한 목록</span>
        <div class="header-actions">
          <el-button link :icon="Plus" @click="onClickAddRole">신규</el-button>
        </div>
      </div>

      <!-- 검색창 (Compact) -->
      <div class="search-box">
        <el-input
            v-model="searchKeyword"
            placeholder="권한명/ID 검색"
            size="small"
            clearable
            @input="onSearchFilter"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <!-- 좌측 그리드 -->
      <div class="grid-wrapper">
        <ag-grid-vue
            class="ag-theme-alpine"
            :theme="'legacy'"
            style="width: 100%; height: 100%;"
            :columnDefs="roleColDefs"
            :rowData="roleList"
            :defaultColDef="defaultColDef"
            rowSelection="single"
            :headerHeight="32"
            :rowHeight="32"
            @grid-ready="onGridReady"
            @cell-clicked="onRoleSelect"
            :getRowId="(params) => params.data.roleId"
        >
        </ag-grid-vue>
      </div>
    </div>

    <!-- [우측 패널] 상세 및 매핑 (Detail) -->
    <div class="panel-right">

      <!-- 선택된 권한이 없을 때 (Empty State) -->
      <div v-if="!selectedRole" class="empty-state">
        <el-empty description="권한을 선택하거나 신규 버튼을 누르세요." :image-size="80" />
      </div>

      <!-- 선택된 권한 상세 -->
      <div v-else class="detail-container">
        <!-- 우측 헤더 -->
        <div class="detail-header">
          <div class="detail-title-group">
            <span class="role-badge">{{ selectedRole.roleId }}</span>
            <span class="role-name">{{ selectedRole.roleName }}</span>
          </div>
          <el-button
              type="danger"
              plain
              size="small"
              :icon="Delete"
              @click="onDeleteRole"
              :disabled="isNewRole"
          >
            삭제
          </el-button>
        </div>

        <!-- 탭 영역 -->
        <el-tabs v-model="activeTab" type="border-card" class="compact-tabs">

          <!-- TAB 1: 기본 정보 -->
          <el-tab-pane label="기본 정보" name="basic">
            <div class="tab-content-scroller">
              <el-form :model="formBasic" label-width="90px" size="small" style="max-width: 400px; margin-top: 10px;">
                <el-form-item label="권한 ID" required>
                  <el-input v-model="formBasic.roleId" :disabled="!isNewRole" placeholder="ex) ADMIN" maxlength="20">
                    <template #prefix><el-icon><Key /></el-icon></template>
                  </el-input>
                </el-form-item>
                <el-form-item label="권한 명" required>
                  <el-input v-model="formBasic.roleName" placeholder="권한 이름" />
                </el-form-item>
                <el-form-item label="설명">
                  <el-input v-model="formBasic.description" type="textarea" :rows="3" placeholder="권한 설명" />
                </el-form-item>
                <el-divider />
                <div style="text-align: right;">
                  <el-button type="primary" size="small" :icon="Check" @click="saveBasicInfo">저장</el-button>
                </div>
              </el-form>
            </div>
          </el-tab-pane>

          <!-- TAB 2: 메뉴 권한 -->
          <el-tab-pane label="메뉴 연결" name="menu">
            <div class="tab-inner-flex">
              <div class="tree-filter-box">
                <el-input v-model="menuFilterText" placeholder="메뉴 검색..." size="small">
                  <template #prefix><el-icon><Search /></el-icon></template>
                </el-input>
              </div>
              <div class="tree-wrapper">
                <el-tree
                    ref="menuTreeRef"
                    :data="menuTreeData"
                    show-checkbox
                    node-key="menuId"
                    default-expand-all
                    :props="{ label: 'menuName' }"
                    :filter-node-method="filterMenuNode"
                    highlight-current
                    :height="300"
                >
                  <template #default="{ node, data }">
                    <span class="custom-tree-node">
                      <span>{{ node.label }}</span>
                      <span class="tree-id-text">{{ data.menuId }}</span>
                    </span>
                  </template>
                </el-tree>
              </div>
              <div class="tab-bottom-action">
                <el-button type="success" size="small" :icon="Check" @click="saveMenuMapping">메뉴 권한 저장</el-button>
              </div>
            </div>
          </el-tab-pane>

          <!-- TAB 3: 사용자 매핑 -->
          <el-tab-pane label="사용자 할당" name="user">
            <div class="tab-inner-flex">
              <div class="transfer-wrapper">
                <!-- 공간 절약을 위해 커스텀 클래스 적용 -->
                <el-transfer
                    v-model="assignedUserIds"
                    :data="allUsersSource"
                    :titles="['대기', '할당됨']"
                    filterable
                    filter-placeholder="검색"
                    :props="{ key: 'userId', label: 'userName' }"
                    class="compact-transfer"
                >
                  <template #default="{ option }">
                    <span style="font-size: 12px;">{{ option.userName }}</span>
                  </template>
                </el-transfer>
              </div>
              <div class="tab-bottom-action">
                <el-button type="success" size="small" :icon="User" @click="saveUserMapping">사용자 매핑 저장</el-button>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<style>
@import "ag-grid-community/styles/ag-grid.css";
@import "ag-grid-community/styles/ag-theme-alpine.css";
</style>

<style scoped>
/* 메인 컨테이너 (820x620 고정, Flex Row) */
.role-management-container {
  display: flex;
  width: 820px;
  height: 620px;
  background-color: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  box-sizing: border-box;
  font-size: 12px;
  margin-top: 4px;
}

/* --- 좌측 패널 (목록) --- */
.panel-left {
  width: 270px; /* 좁게 설정 */
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--el-border-color-light);
  background-color: var(--el-fill-color-lighter);
}

.panel-header {
  height: 40px; /* 헤더 높이 축소 */
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px;
  border-bottom: 1px solid var(--el-border-color-light);
  background-color: var(--el-bg-color);
}

.panel-title {
  font-weight: 700;
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.search-box {
  padding: 6px 8px;
  background-color: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
}

.grid-wrapper {
  flex: 1;
  width: 100%;
}

/* --- 우측 패널 (상세) --- */
.panel-right {
  flex: 1; /* 나머지 공간 */
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color);
  overflow: hidden;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.detail-header {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.role-badge {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 700;
  font-size: 11px;
  margin-right: 6px;
  font-family: monospace;
}
.role-name {
  font-weight: bold;
  font-size: 13px;
}

/* 탭 스타일 최적화 (Compact) */
.compact-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: none;
  box-shadow: none;
}
:deep(.el-tabs__header) {
  margin-bottom: 0;
  background-color: var(--el-fill-color-light);
}
:deep(.el-tabs__item) {
  font-size: 14px !important;
  height: 32px;
  line-height: 32px;
}
:deep(.el-tabs__content) {
  flex: 1;
  padding: 12px !important; /* 패딩 축소 */
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 탭 내부 공통 */
.tab-content-scroller {
  flex: 1;
  overflow-y: auto;
}
.tab-inner-flex {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* 메뉴 트리 관련 */
.tree-filter-box {
  margin-bottom: 8px;
}
.tree-wrapper {
  flex: 1;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  overflow-y: auto;
  padding: 4px;
}
.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 8px;
  font-size: 12px;
}
.tree-id-text {
  font-size: 10px;
  color: #999;
}

/* Transfer 관련 (오밀조밀하게 강제 조정) */
.transfer-wrapper {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 10px;
}
/* Transfer 내부 패널 사이즈 강제 조정 */
:deep(.el-transfer-panel) {
  width: 180px; /* 패널 폭 줄임 */
}
:deep(.el-transfer-panel__header) {
  height: 32px;
  line-height: 32px;
}
:deep(.el-transfer-panel__body) {
  height: 280px; /* 높이 고정 */
}
:deep(.el-transfer__buttons) {
  padding: 0 8px;
}
:deep(.el-button--small) {
  padding: 5px 8px;
}

.tab-bottom-action {
  margin-top: 8px;
  text-align: right;
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 8px;
}
</style>

<style>
/* 다크모드 미세 조정 */
html.dark .panel-left {
  background-color: #1d1e22;
  border-color: var(--el-border-color);
}
html.dark .role-management-container {
  border-color: var(--el-border-color);
}
html.dark .ag-theme-alpine {
  --ag-header-height: 32px;
  --ag-row-height: 32px;
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