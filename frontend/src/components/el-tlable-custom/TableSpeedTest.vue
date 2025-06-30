<script setup lang="ts">
import {Api} from "@/api/axiosInstance";
import {ApiUrls} from "@/api/apiUrls";
import {reactive, nextTick} from 'vue';
import TableColumn, {ColumnDef} from "@/components/el-tlable-custom/TableColumn.vue";
import VirtualizedTable from "@/components/el-tlable-custom/VirtualizedTable.vue";

// define state
const state = reactive({
  // ★★★ 변경점 1: 데이터 소스를 두 개의 독립적인 변수로 분리합니다.
  elTableData: [] as any[],
  virtualizedTableData: [] as any[],

  isLoading: false,
  performance: {
    fetchTime: 0,
    elTableRenderTime: 0,
    virtualizedTableRenderTime: 0,
  }
});

// column define (변경 없음)
const columns1: ColumnDef[] = [
  {prop: 'userId', label: '유저ID', width: 130, align: "center"},
  {prop: 'userName', label: '유저명', minWidth: 170, align: "center"},
];

/**
 * 초기화
 */
const onClickPageClear = () => {
  // 성능시간 초기화
  state.performance.fetchTime = 0;
  state.performance.elTableRenderTime = 0;
  state.performance.virtualizedTableRenderTime = 0;

  // 데이터 초기화
  state.elTableData = [];
  state.virtualizedTableData = [];
}

/**
 * 조회버튼 클릭 이벤트
 */
const onClickSearchPaging = async () => {
  // 이전 결과 초기화 및 로딩 상태 시작
  state.elTableData = [];
  state.virtualizedTableData = [];
  state.performance = { fetchTime: 0, elTableRenderTime: 0, virtualizedTableRenderTime: 0 };
  state.isLoading = true;

  // --- 1. 데이터 로딩 시간 측정 ---
  const fetchStart = performance.now();
  const retData = await Api.post(ApiUrls.TABLE_TEST, {}, false);
  const fetchEnd = performance.now();
  state.performance.fetchTime = fetchEnd - fetchStart;

  if (!retData || !retData.data) {
    state.isLoading = false;
    return;
  }

  // API로부터 받은 원본 데이터를 변수에 저장
  const originalData = retData.data;

  // =================================================================
  // ★★★ 변경점 2: 렌더링 시간 측정 로직을 완전히 분리.
  // =================================================================

  // --- 2. el-table 렌더링 시간 측정 ---
  // 타이머 시작
  const elTableRenderStart = performance.now();
  // el-table에만 데이터 할당
  state.elTableData = originalData;
  // el-table의 DOM 업데이트가 완료될 때까지 대기
  await nextTick();
  // 타이머 종료 및 시간 기록
  const elTableRenderEnd = performance.now();
  state.performance.elTableRenderTime = elTableRenderEnd - elTableRenderStart;


  // --- 3. el-table-v2 렌더링 시간 측정 ---
  // 타이머 시작
  const virtualizedTableRenderStart = performance.now();
  // el-table-v2에만 데이터 할당
  state.virtualizedTableData = originalData;
  // el-table-v2의 DOM 업데이트가 완료될 때까지 대기
  await nextTick();
  // 타이머 종료 및 시간 기록
  const virtualizedTableRenderEnd = performance.now();
  state.performance.virtualizedTableRenderTime = virtualizedTableRenderEnd - virtualizedTableRenderStart;

  // 모든 측정 및 렌더링 작업이 끝났으므로 로딩 상태 종료
  state.isLoading = false;

  console.log('성능 측정 결과 (개별 측정):', state.performance);
}
</script>

<template>
  <div>
    <!-- 상단 툴바 (변경 없음) -->
    <div style="flex: 1; padding: 4px;">
      <div class="toolbar">

        <div class="right-group">
          <el-button icon="Search" style="font-weight: bold; color: #001233;" @click="onClickSearchPaging" :loading="state.isLoading">
            성능 측정
          </el-button>
          <el-button type="info" plain icon="Refresh" size="small" @click="onClickPageClear">
            초기화
          </el-button>
        </div>
        <div>
          <div>
            <el-alert type="info" :closable="false" style="width: 100%; margin-bottom: 2px;">
              <el-text style="font-size: 10px; text-align: left; display: block;">
                렌더링 시간은<br>
                '데이터 할당 시작'부터 'DOM 업데이트 완료'까지의 시간입니다.
              </el-text>
            </el-alert>
          </div>
          <div>
            <el-descriptions :column="3" border size="small" style="font-size: 12px;">
              <el-descriptions-item label="데이터 로딩">
                {{ state.performance.fetchTime.toFixed(2) }} ms
              </el-descriptions-item>
              <el-descriptions-item label="el-table 렌더링">
                <span style="color: blue; font-weight: bold;">{{ state.performance.elTableRenderTime.toFixed(2) }} ms</span>
              </el-descriptions-item>
              <el-descriptions-item label="el-table-v2 렌더링">
                <span style="color: green; font-weight: bold;">{{ state.performance.virtualizedTableRenderTime.toFixed(2) }} ms</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </div>
    </div>

    <!-- 테이블 영역 -->
    <div style="display: flex; width: 100%;">

      <!-- el-table -->
      <div style="flex: 1; background-color: #eaf2ff; padding: 10px;">
        <div class="toolbar">
          <el-tag effect="plain" size="large" style="font-weight: bold;">
            el-table
          </el-tag>
        </div>
        <!-- ★★★ 변경점 3: :data에 elTableData를 바인딩합니다. -->
        <el-table v-loading="state.isLoading" stripe border highlight-current-row :data="state.elTableData" style="height: 170px;">
          <TableColumn :columns="columns1"/>
        </el-table>
      </div>

      <!-- el-table-v2 -->
      <div style="flex: 1; background-color: #e6f7ea; padding: 10px; margin-left: 2px;">
        <div class="toolbar">
          <el-tag effect="plain" size="large" style="font-weight: bold;">
            table-v2
          </el-tag>
        </div>
        <div style="height: 170px; width: 330px;" v-loading="state.isLoading">
          <!-- ★★★ 변경점 4: :data에 virtualizedTableData를 바인딩합니다. -->
          <VirtualizedTable
              :data="state.virtualizedTableData"
              :columns="columns1"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px; /* 여유 있는 간격 */
}
.right-group {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-right: auto; /* 왼쪽으로 밀기 */
  gap: 2px; /* 스위치와 버튼 사이 간격 */
}
</style>

<!--
<el-card shadow="never" style="margin-bottom: 20px;">
<template #header>
  <div class="card-header">
    <span>렌더링 성능 측정 결과</span>
  </div>
</template>
<div style="display: flex; gap: 20px; font-size: 14px;">
  <span><strong>데이터 로딩:</strong> {{ state.performance.fetchTime.toFixed(2) }} ms</span>
  <span><strong>el-table 렌더링:</strong> <span style="color: blue; font-weight: bold;">{{ state.performance.elTableRenderTime.toFixed(2) }} ms</span></span>
  <span><strong>el-table-v2 렌더링:</strong> <span style="color: green; font-weight: bold;">{{ state.performance.virtualizedTableRenderTime.toFixed(2) }} ms</span></span>
</div>
<el-alert title="렌더링 시간은 '데이터 할당 시작'부터 'DOM 업데이트 완료'까지의 시간입니다. 정확한 비교를 위해 수천 건 이상의 대용량 데이터로 테스트하세요." type="info" :closable="false" style="margin-top: 10px;"/>
</el-card>
-->