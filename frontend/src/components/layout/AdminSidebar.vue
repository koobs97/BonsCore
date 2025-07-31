<template>
  <el-aside width="220px" class="admin-sidebar">
    <el-scrollbar>
      <el-menu
          default-active="1011"
          router
          class="admin-menu"
      >
        <template v-for="item in menuItems" :key="item.id">
          <!-- 하위 메뉴가 있는 경우 -->
          <el-sub-menu v-if="item.children && item.children.length > 0" :index="String(item.id)">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.name }}</span>
            </template>
            <el-menu-item
                v-for="child in item.children"
                :key="child.id"
                :index="child.url"
            >
              <el-icon><component :is="child.icon" /></el-icon>
              <span>{{ child.name }}</span>
            </el-menu-item>
          </el-sub-menu>

          <!-- 하위 메뉴가 없는 경우 -->
          <el-menu-item v-else :index="item.url">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.name }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>
  </el-aside>
</template>

<script setup>
import { defineProps } from 'vue';

// 부모 컴포넌트로부터 메뉴 데이터 받기
defineProps({
  menuItems: {
    type: Array,
    required: true
  }
});
</script>

<style scoped>
.admin-sidebar {
  height: calc(100vh - 50px - 60px); /* 전체 높이 - 헤더 - 푸터(임의값) */
  max-height: 670px; /* page-container의 max-height에 맞게 조정 */
  background-color: #ffffff;
  border-right: 1px solid #e4e7ed;
  flex-shrink: 0; /* 너비가 줄어들지 않도록 설정 */
}

.admin-menu {
  border-right: none; /* el-menu 기본 우측 테두리 제거 */
}
</style>