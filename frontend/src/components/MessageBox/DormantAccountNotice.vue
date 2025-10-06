<script setup lang="ts">
import { Lock } from "@element-plus/icons-vue"; // 휴면 '계정'이므로 '잠금' 아이콘도 잘 어울립니다.

defineProps({
  title: String,
  message: String,
});
</script>

<template>
  <div class="gradient-notice-container">
    <!-- 비주얼 헤더 영역 -->
    <div class="visual-header">
      <el-icon :size="60">
        <Lock />
      </el-icon>
    </div>

    <!-- 텍스트 컨텐츠 영역 -->
    <div class="text-content">
      <h3 class="confirm-title">{{ title || '계정이 비활성화되었습니다' }}</h3>
      <p class="confirm-message" v-html="message || '장기간 서비스를 이용하지 않아 회원님의 계정이 휴면 상태로 전환되었습니다. 본인인증 후 즉시 정상적으로 이용하실 수 있습니다.'"></p>
    </div>
  </div>
</template>

<style scoped>
.gradient-notice-container {
  text-align: center;
}

.visual-header {
  padding: 32px 24px;
  background: linear-gradient(135deg, #fdfcfb 0%, #e2d1c3 100%);
  border-top-left-radius: 6px;
  border-top-right-radius: 6px;
  display: flex;
  justify-content: center;
  align-items: center;
}

html.dark .visual-header {
  background: linear-gradient(135deg, #4a4a4a 0%, #2c2c2c 100%);
}

.visual-header .el-icon {
  color: #8c6a4a;
  filter: drop-shadow(0 4px 6px rgba(0, 0, 0, 0.1));
}

html.dark .visual-header .el-icon {
  color: #c8a98a;
}

.text-content {
  padding: 24px 10px;
}

.confirm-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 12px;
  color: var(--el-text-color-primary);
}

.confirm-message {
  font-size: 15px;
  line-height: 1.7;
  margin: 0;
  color: var(--el-text-color-secondary);
}
</style>
<style>
/* 휴면 계정 알림창 전용 스타일 */
.dormant-account-box {
  --el-messagebox-width: 480px;
  padding: 0 !important; /* 내부 컴포넌트 디자인이 깨지지 않도록 기본 패딩을 모두 제거합니다. */
  border-radius: 8px; /* 모서리를 조금 더 둥글게 */
}
.el-message-box__header {
  padding-bottom: 0;
}
.dormant-account-box .el-message-box__header {
  display: none; /* 커스텀 컴포넌트가 제목을 포함하므로 기본 헤더는 숨깁니다. */
}

.dormant-account-box .el-message-box__content {
  padding: 0; /* 내용 영역의 패딩도 제거하여 커스텀 컴포넌트가 꽉 차게 합니다. */
}

.dormant-account-box .el-message-box__btns {
  padding: 16px 24px 24px; /* 버튼 영역의 패딩은 적절하게 조절합니다. */
}
</style>