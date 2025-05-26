<script setup lang="ts">
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import { reactive, ref } from 'vue';
import TableColumn from "@/components/TableColumn.vue";

interface Paging {
  userId: string,
  userName: string,
  email: string
}

// define state
const state = reactive({
  list1: [] as any
});

const columns1 = ref([
  { prop: 'userId', label: '유저ID', width: 110 },
  { prop: 'userName', label: '유저명', width: 300 },
  { prop: 'email', label: 'email', width: 300 },
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
    <h2>로그인 성공</h2>
  </div>
  <div>
    <el-card shadow="never">
      <el-text>Paging example</el-text>
      <el-button icon="Search" @click="onClickSearchPaging">조회</el-button>
      <el-table :data="state.list1" style="height: 200px;">
        <TableColumn :columns="columns1" />
      </el-table>
    </el-card>
  </div>

</template>

<style scoped>

</style>