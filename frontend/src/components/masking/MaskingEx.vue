<script setup lang="ts">
/**
 * ========================================
 * 파일명   : MaskingEx.vue
 * ----------------------------------------
 * 설명     : 마스킹 사용 예시를 위한 컴포넌트
 * 작성자   : koobonsang
 * 버전     : 1.0
 * 작성일자 : 2025-05-30
 * ========================================
 */

// import
import {Api} from "@/api/axiosInstance";
import {ApiUrls} from "@/api/apiUrls";
import {reactive} from 'vue';
import TableColumn, { ColumnDef } from "@/components/el-tlable-custom/TableColumn.vue";

// paging request vo
interface MaskingVo {
  maskingEnabled: boolean;
}

// define state
const state = reactive({
  maskingVo: {
    maskingEnabled: true,
  } as MaskingVo,
  list1: [] as any,
});

// column define
const columns1: ColumnDef[] = [
  { prop: 'userId', label: '유저ID', width: 200, align: "center" },
  { prop: 'userName', label: '유저명', width: 150, align: "center" },
  { prop: 'email', label: 'email', minWidth: 250, align: "center" },
];

/**
 * 조회버튼 클릭 이벤트
 */
const onClickSearchPaging = async () => {

  const retData = await Api.post(ApiUrls.MASKING, state.maskingVo, true);
  if (!retData) return;

  console.log(retData)

  state.list1 = retData.data.list
}
</script>

<template>
    <div class="toolbar">
      <el-tag effect="plain" size="large" style="font-weight: bold;">
        마스킹 샘플
      </el-tag>
      <div class="right-group">
      <el-form-item
          label="마스킹"
          style="margin-top: 3px; margin-bottom: 0;">
        <el-switch
            v-model="state.maskingVo.maskingEnabled"
            class="ml-2"
            inline-prompt
            active-text="Y"
            inactive-text="N"
        />
      </el-form-item>
      <el-button icon="Search" style="font-weight: bold; color: #001233;" @click="onClickSearchPaging">조회</el-button>
        </div>
    </div>

    <el-table stripe border highlight-current-row :data="state.list1" style="height: 270px;">
      <TableColumn :columns="columns1" />
    </el-table>
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