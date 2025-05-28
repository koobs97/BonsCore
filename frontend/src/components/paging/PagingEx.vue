<script setup lang="ts">
import {Api} from "@/api/axiosInstance";
import {ApiUrls} from "@/api/apiUrls";
import {reactive, ref} from 'vue';
import TableColumn from "@/components/TableColumn.vue";
import {Page} from "@/common/types/Page";

interface SampleVo {
  page: Page;
  userId: string;
  userName: string;
  email: string;
}

// define state
const state = reactive({
  list1: [] as any,
  sampleVo: {} as SampleVo,
  page: {
    total: 0,
    pageNum: 1,
    pageSize: 10,
  } as Page,
});

const columns1 = ref([
  { prop: 'userId', label: '유저ID', width: 110, align: "center" },
  { prop: 'userName', label: '유저명', width: 300, align: "center" },
  { prop: 'email', label: 'email', width: 300, align: "center" },
]);

const onClickSearchPaging = async () => {

  state.sampleVo.page = {pageNum: 1, pageSize: 10} as Page;

  const retData = await Api.post(ApiUrls.PAGING, state.sampleVo, true);

  state.page.total = retData.data.total;
  state.page.pageSize = state.sampleVo.page.pageSize;
  state.page.pageNum = state.sampleVo.page.pageNum;

  state.list1 = retData.data.list
  console.log(state.list1)
}

const handleOnChange = async (currentPage: number) => {

  state.sampleVo.page = {pageNum: currentPage, pageSize: 10} as Page;

  const retData = await Api.post(ApiUrls.PAGING, state.sampleVo, true);

  state.page.total = retData.data.total;
  state.page.pageSize = state.sampleVo.page.pageSize;

  state.list1 = retData.data.list
  console.log(state.list1)
}

</script>

<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-tag effect="plain" size="large">
          Paging example
        </el-tag>
        <el-button icon="Search" type="info" plain @click="onClickSearchPaging">조회</el-button>
      </div>
      <el-table border highlight-current-row :data="state.list1" style="height: 200px;">
        <TableColumn :columns="columns1" />
      </el-table>
      <el-pagination
          background
          size="small"
          layout="prev, pager, next, sizes, total"
          :total="state.page.total"
          v-model:page-size="state.page.pageSize"
          v-model:current-page="state.page.pageNum"
          @current-change="handleOnChange"
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