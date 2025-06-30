<script setup lang="ts">
import PagingEx from "@/components/paging/PagingEx.vue";
import MaskingEx from "@/components/masking/MaskingEx.vue";
import TimeViewr from "@/components/time/TimeViewr.vue";
import UserInfoAvatar from "@/components/login/userInfoAvatar.vue";
import springIcon from '@/assets/images/spring-icon.svg'
import { SegmentedProps } from "element-plus";
import { ref } from "vue";
import { useRouter } from 'vue-router'
import TableSpeedTest from "@/components/el-tlable-custom/TableSpeedTest.vue";
import TheFooter from "@/components/layout/TheFooter.vue";

const router = useRouter()

const size = ref<SegmentedProps['size']>('Dash Board' as any)
const sizeOptions = ['Dash Board']

const onClickRefresh = () => {
  router.push("/");
  location.href = '/';
}
</script>

<template>

  <div style="display: flex;">

    <!-- 시계영역 -->
    <div style="text-align: left; margin-right: 4px; width: 110px;">
      <TimeViewr />
    </div>
    <!-- 시계영역 -->

    <!-- 중탕 타이틀 영역 -->
    <el-card shadow="never" style="height: 90px; width: calc(100% - 352px); margin-bottom: 4px; position: relative;">
      <template #default>

        <!-- 중앙 텍스트 -->
        <div style="height: 100%; margin: 10px 0 0 0; display: flex; flex-direction: column; justify-content: center; align-items: center;">
          <div style="display: flex; align-items: center;">
            <el-image
                style="border: 2px solid var(--el-color-primary); border-radius: 4px; padding: 3px;"
                :src="springIcon" />
            <el-segmented
                v-model="size"
                :options="sizeOptions"
                style="font-size: 24px; font-weight: bold; margin: 0 0 0 2px;"
                @click="onClickRefresh"
            />
          </div>
        </div>

      </template>
    </el-card>
    <!-- 중탕 타이틀 영역 -->

    <!-- 유저 card 영역 -->
    <UserInfoAvatar />
  </div>

  <!-- main 영역 -->
  <el-tabs type="border-card" class="demo-tabs" style="height: 420px;">
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
    <el-tab-pane label="EL테이블">
      <div style="width: 700px;">
        <TableSpeedTest/>
      </div>
    </el-tab-pane>
  </el-tabs>
  <!-- main 영역 -->

  <!-- Footer -->
  <TheFooter />
  <!-- Footer -->

</template>

<style scoped>
/* UserInfo 에서 사용중인 스타일 */
.custom-el-card {
  --el-card-border-color: var(--el-border-color-light);
  --el-card-border-radius: 4px;
  --el-card-padding: 0px;
  --el-card-bg-color: var(--el-fill-color-blank);
  background-color: var(--el-card-bg-color);
  border: 1px solid var(--el-card-border-color);
  border-radius: var(--el-card-border-radius);
  color: var(--el-text-color-primary);
  overflow: hidden;
  transition: var(--el-transition-duration);
}
</style>