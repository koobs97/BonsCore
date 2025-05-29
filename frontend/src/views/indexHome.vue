<script setup lang="ts">
import PagingEx from "@/components/paging/PagingEx.vue";
import MaskingEx from "@/components/masking/MaskingEx.vue";
import {Reading} from "@element-plus/icons-vue";
</script>

<template>
  <div>
    <h2>로그인 성공</h2>
  </div>

  <el-tabs type="border-card" class="demo-tabs" style="height: 350px;">
    <el-tab-pane label="페이징">
      <div style="width: 700px;">
        <PagingEx/>
      </div>
    </el-tab-pane>
    <el-tab-pane label="마스킹">
      <div style="width: 700px;">
        <MaskingEx/>
      </div>
    </el-tab-pane>
  </el-tabs>
</template>

<style scoped>

</style>