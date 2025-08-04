<template>
  <div class="activity-log-container">
    <!-- 1. 페이지 헤더 -->
    <div class="page-header">
      <h3>
        <el-icon><Monitor /></el-icon> 사용자 활동 로그
      </h3>
    </div>

    <!-- 2. 검색 패널 -->
    <el-card class="search-panel" shadow="never">
      <el-form :model="searchParams" label-position="top" inline>
        <el-form-item label="조회 기간">
          <el-date-picker
              v-model="searchParams.dateRange"
              type="datetimerange"
              range-separator="~"
              start-placeholder="시작일"
              end-placeholder="종료일"
              :shortcuts="dateShortcuts"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="사용자 ID">
          <el-input v-model="searchParams.userId" placeholder="사용자 ID 입력" clearable />
        </el-form-item>
        <el-form-item label="활동 유형">
          <el-select v-model="searchParams.activityType" placeholder="전체" clearable>
            <el-option label="로그인" value="LOGIN" />
            <el-option label="로그아웃" value="LOGOUT" />
            <el-option label="회원가입" value="SIGNUP" />
            <el-option label="ID 찾기" value="FIND_ID" />
            <el-option label="PW 찾기" value="FIND_PW" />
          </el-select>
        </el-form-item>
        <el-form-item label="결과">
          <el-select v-model="searchParams.activityResult" placeholder="전체" clearable>
            <el-option label="성공" value="SUCCESS" />
            <el-option label="실패" value="FAILURE" />
          </el-select>
        </el-form-item>
        <el-form-item label=" ">
          <div class="search-buttons">
            <el-button type="primary" :icon="Search" @click="onSearch">조회</el-button>
            <el-button :icon="Refresh" @click="onReset">초기화</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 3. 데이터 그리드 -->
    <div class="grid-container">
      <ag-grid-vue
          class="ag-theme-quartz"
          style="width: 100%; height: 100%;"
          :columnDefs="colDefs"
          :rowData="rowData"
          :defaultColDef="defaultColDef"
          :pagination="true"
          :paginationPageSize="20"
          :paginationPageSizeSelector="[20, 50, 100]"
          @grid-ready="onGridReady"
      >
      </ag-grid-vue>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-alpine.css'
import { AgGridVue } from 'ag-grid-vue3'
// main.ts에서 전역으로 로드했으므로 스타일 import는 필요 없습니다.
import axios from 'axios';
import { ElMessage, ElLoading } from 'element-plus';
// 아이콘은 main.ts에서 전역 등록했으므로 개별 import가 필요 없을 수 있습니다.
// 만약 안된다면 아래 주석을 해제하세요.
// import { Monitor, Search, Refresh } from '@element-plus/icons-vue';

// --- 상태 변수 및 Ref ---
const gridApi = ref(null);
const searchParams = reactive({ dateRange: null, userId: '', activityType: '', activityResult: '' });
const rowData = ref([]);
const defaultColDef = { resizable: true, sortable: true, filter: true, floatingFilter: true, flex: 1 };

// --- 그리드 컬럼 정의 ---
const colDefs = ref([
  { headerName: 'ID', field: 'logId', width: 90, flex: 0, sort: 'desc' },
  { headerName: '시간', field: 'createdAt', width: 180,
    valueFormatter: p => p.value ? new Date(p.value).toLocaleString('ko-KR') : ''
  },
  { headerName: '사용자 ID', field: 'userId', width: 150 },
  { headerName: '활동 유형', field: 'activityType', width: 130 },
  {
    headerName: '결과',
    field: 'activityResult',
    width: 100,
    cellRenderer: (params) => {
      if (params.value === 'SUCCESS') return `<span style="color: #67c23a;">● 성공</span>`;
      if (params.value === 'FAILURE') return `<span style="color: #f56c6c;">● 실패</span>`;
      return params.value;
    }
  },
  { headerName: '요청 IP', field: 'requestIp', width: 150 },
  { headerName: '요청 URI', field: 'requestUri', minWidth: 200 },
  { headerName: '메소드', field: 'requestMethod', width: 100, flex: 0 },
  {
    headerName: '에러 메시지',
    field: 'errorMessage',
    minWidth: 250,
    tooltipValueGetter: (p) => p.value, // 툴팁으로 전체 메시지 보기
  },
  {
    headerName: 'User-Agent',
    field: 'userAgent',
    minWidth: 300,
    tooltipValueGetter: (p) => p.value,
  },
]);

// --- 함수 ---
const onGridReady = (params) => { gridApi.value = params.api; };

const fetchLogs = async () => {
  const loadingInstance = ElLoading.service({ target: '.grid-container', text: '데이터를 불러오는 중입니다...' });
  try {
    const apiParams = {
      startDate: searchParams.dateRange ? searchParams.dateRange[0] : null,
      endDate: searchParams.dateRange ? searchParams.dateRange[1] : null,
      userId: searchParams.userId,
      activityType: searchParams.activityType,
      activityResult: searchParams.activityResult,
    };

    // [실제 API 호출] '/api/admin/logs' 엔드포인트를 실제 경로로 수정하세요.
    // const response = await axios.get('/api/admin/logs', { params: apiParams });
    // rowData.value = response.data;

    // [테스트용 목업 데이터] API 연동 전까지 사용하세요.
    await new Promise(resolve => setTimeout(resolve, 500));
    rowData.value = generateMockData(apiParams);

  } catch (error) {
    console.error("로그 데이터 조회 실패:", error);
    ElMessage.error('데이터를 불러오는 중 오류가 발생했습니다.');
  } finally {
    loadingInstance.close();
  }
};

const onSearch = () => fetchLogs();
const onReset = () => {
  Object.assign(searchParams, { dateRange: null, userId: '', activityType: '', activityResult: '' });
  fetchLogs();
};

onMounted(fetchLogs);

const dateShortcuts = [
  { text: '오늘', value: () => { const end = new Date(); const start = new Date(); start.setHours(0, 0, 0, 0); return [start, end] } },
  { text: '어제', value: () => { const end = new Date(); const start = new Date(); start.setDate(start.getDate() - 1); end.setDate(end.getDate() - 1); start.setHours(0,0,0,0); end.setHours(23,59,59,999); return [start, end] } },
  { text: '최근 7일', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 7); return [start, end] } },
]

// --- 테스트용 목업 데이터 생성 함수 ---
const generateMockData = (params) => {
  const sampleData = [];
  const types = ['LOGIN', 'LOGOUT', 'SIGNUP', 'FIND_ID'];
  const results = ['SUCCESS', 'FAILURE'];
  const users = ['user01', 'admin', 'testuser', 'guest', 'user02'];

  for (let i = 1; i <= 50; i++) {
    const result = results[Math.floor(Math.random() * results.length)];
    sampleData.push({
      logId: 1000 + i,
      createdAt: new Date(new Date().getTime() - Math.random() * 1000 * 3600 * 24 * 7).toISOString(),
      userId: users[Math.floor(Math.random() * users.length)],
      activityType: types[Math.floor(Math.random() * types.length)],
      activityResult: result,
      requestIp: `192.168.0.${i}`,
      requestUri: result === 'SUCCESS' ? '/api/auth/login' : '/api/auth/login-fail',
      requestMethod: 'POST',
      errorMessage: result === 'FAILURE' ? '비밀번호가 일치하지 않습니다.' : null,
      userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
    });
  }
  return sampleData.filter(d =>
      (!params.userId || d.userId.includes(params.userId)) &&
      (!params.activityType || d.activityType === params.activityType) &&
      (!params.activityResult || d.activityResult === params.activityResult)
  );
};
</script>

<style scoped>
.activity-log-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px); /* 실제 레이아웃에 맞게 높이 조절 */
}
.page-header { margin-bottom: 20px; }
.page-header h3 { font-size: 1.5rem; display: flex; align-items: center; gap: 8px; }
.search-panel { margin-bottom: 20px; background-color: #fafafa; border: 1px solid #ebeef5; }
.el-form-item { margin-bottom: 10px; }
.search-buttons { display: flex; gap: 10px; padding-top: 5px; }
.grid-container { flex-grow: 1; width: 100%; }
</style>