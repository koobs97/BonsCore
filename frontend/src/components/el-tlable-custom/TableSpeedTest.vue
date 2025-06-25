<script setup lang="ts">
import {Api} from "@/api/axiosInstance";
import {ApiUrls} from "@/api/apiUrls";
import {reactive} from 'vue';
import TableColumn, { ColumnDef } from "@/components/el-tlable-custom/TableColumn.vue";
import VirtualizedTable from "@/components/el-tlable-custom/VirtualizedTable.vue";

// define state
const state = reactive({
  list1: [] as any,
});

// column define
const columns1: ColumnDef[] = [
  { prop: 'userId', label: '유저ID', width: 130, align: "center" },
  { prop: 'userName', label: '유저명', minWidth: 170, align: "center" },
];

/**
 * 조회버튼 클릭 이벤트
 */
const onClickSearchPaging = async () => {

  const retData = await Api.post(ApiUrls.TABLE_TEST, {}, true);
  if (!retData) return;

  console.log(retData)

  state.list1 = retData.data
}
</script>

<template>
  <div>
    <div style="flex: 1; padding: 4px;">
      <div class="toolbar">
        <div class="right-group">
          <el-button icon="Search" style="font-weight: bold; color: #001233;" @click="onClickSearchPaging">조회</el-button>
        </div>
      </div>
    </div>
  </div>
  <div style="display: flex; width: 100%;">
    <div style="flex: 1; background-color: lightblue; padding: 20px;">
      <div class="toolbar">
        <el-tag effect="plain" size="large" style="font-weight: bold;">
          el-table
        </el-tag>
      </div>

      <el-table stripe border highlight-current-row :data="state.list1" style="height: 200px;">
        <TableColumn :columns="columns1" />
      </el-table>
    </div>

    <div style="flex: 1; background-color: lightgreen; padding: 20px;">
      <div class="toolbar">
        <el-tag effect="plain" size="large" style="font-weight: bold;">
          table-v2
        </el-tag>
      </div>
      <div style="height: 200px; width: 300px;">
        <VirtualizedTable
            :data="state.list1"
            :columns="columns1"
        />
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
  align-items: center;
  margin-left: auto; /* 오른쪽으로 밀기 */
  gap: 8px; /* 스위치와 버튼 사이 간격 */
}
</style>