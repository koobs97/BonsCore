<script setup lang="ts">
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { reactive, ref } from 'vue';
import TableColumn from "@/components/TableColumn.vue";

// define state
const state = reactive({
  list1: [] as any
});

const columns1 = ref([
  { prop: 'userId', label: '유저ID', width: 110, align: "center" },
  { prop: 'userName', label: '유저명', width: 300, align: "center" },
  { prop: 'email', label: 'email', width: 300, align: "center" },
]);

const onClickSearchPaging = async () => {
  const retData = await Api.post(ApiUrls.PAGING, {}, true);
  console.log(retData)

  state.list1 = retData.data
  console.log(state.list1)
}

</script>

<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-text>Paging example</el-text>
        <el-button icon="Search" @click="onClickSearchPaging">조회</el-button>
      </div>
      <el-table border highlight-current-row :data="state.list1" style="height: 200px;">
        <TableColumn :columns="columns1" />
      </el-table>
      <el-pagination
          background
          size="small"
          layout="prev, pager, next, sizes, total"
          :total="50"
          class="mt-4"
          style="margin-top: 8px;"
      />
    </el-card>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px; /* 여유 있는 간격 */
}
</style>