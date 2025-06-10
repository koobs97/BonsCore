<script setup lang="ts">
import PagingEx from "@/components/paging/PagingEx.vue";
import MaskingEx from "@/components/masking/MaskingEx.vue";
import TimeViewr from "@/components/TimeViewr.vue";
import UserInfoAvatar from "@/components/login/userInfoAvatar.vue";

</script>

<template>

  <div style="display: flex;">
    <el-card shadow="never" style="height: 90px; width: calc(100% - 115px); margin-bottom: 4px;">
      <template #default>
        <div style="padding: 8px;">
          <h2 style="margin: 0px;">로그인 성공</h2>
        </div>
      </template>
    </el-card>

    <div style="text-align: right; width: 110px;">
      <TimeViewr />
    </div>

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