<script setup lang="ts">
/**
 * ========================================
 * 파일명   : PagingEx.vue
 * ----------------------------------------
 * 설명     : 페이징 사용 예시를 위한 컴포넌트
 * 작성자   : koobonsang
 * 버전     : 1.0
 * 작성일자 : 2025-05-28
 * ========================================
 */

// import
import {Api} from "@/api/axiosInstance";
import {ApiUrls} from "@/api/apiUrls";
import {reactive, ref} from 'vue';
import TableColumn, { ColumnDef } from "@/components/TableColumn.vue";
import {Page} from "@/common/types/Page";

// paging request vo
interface PagingVo {
  page: Page;
  userId: string;
  userName: string;
  email: string;
}

// define state
const state = reactive({
  list1: [] as any,
  pagingVo: {} as PagingVo,
  page: {
    total: 0,
    pageNum: 1,
    pageSize: 10,
  } as Page,
});

// column define
const columns1: ColumnDef[] = [
  { prop: 'userId', label: '유저ID', width: 110, align: "center" },
  { prop: 'userName', label: '유저명', width: 300, align: "center" },
  { prop: 'email', label: 'email', width: 300, align: "center" },
];

/**
 * 조회버튼 클릭 이벤트
 */
const onClickSearchPaging = async () => {

  state.pagingVo.page = { pageNum: 1, pageSize: 10 } as Page;
  const retData = await Api.post(ApiUrls.PAGING, state.pagingVo, true);

  state.page.total = retData.data.total;
  state.page.pageSize = state.pagingVo.page.pageSize;
  state.page.pageNum = state.pagingVo.page.pageNum;

  state.list1 = retData.data.list
}

/**
 * 페이지 num 클릭 이벤트
 * @param currentPage
 */
const handleOnChange = async (currentPage: number) => {

  state.pagingVo.page = {pageNum: currentPage, pageSize: 10} as Page;
  const retData = await Api.post(ApiUrls.PAGING, state.pagingVo, true);

  state.page.total = retData.data.total;
  state.page.pageSize = state.pagingVo.page.pageSize;

  state.list1 = retData.data.list
}

</script>

<template>
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
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px; /* 여유 있는 간격 */
}
</style>