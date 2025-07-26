<script setup>
import { ref, shallowRef } from 'vue';
import { Search, Compass, Clock, ChatDotRound, Odometer, UserFilled } from '@element-plus/icons-vue';
import TheFooter from "@/components/layout/TheFooter.vue";
import UserInfoAvatar from "@/components/login/userInfoAvatar.vue";

// 로그인한 사용자 이름 (임시 데이터)
const userName = ref('개발자');

// 검색어 데이터
const searchQuery = ref('');

// 활성화된 탭 관리
const activeTab = ref('prediction');

// DB에서 조회했다고 가정한 임시 메뉴 데이터 (프로토타입에 맞게 설명 수정)
const menuItems = shallowRef([
  { id: 1, name: 'prediction', label: '시간대별 예측', icon: Clock, description: '축적된 데이터를 기반으로 시간대별 대기 시간을 예측합니다.' },
  { id: 2, name: 'community', label: '저장소', icon: ChatDotRound, description: '나만의 데이터 저장' },
]);
</script>

<template>
  <div class="page-container">
    <!-- 상단 네비게이션 바 (로그인 상태) -->
    <el-header class="main-header">
      <div class="logo">
        <el-icon :size="24" color="#001233" class="logo-icon"><Odometer /></el-icon>
        <span class="logo-text">웨이팅 레이더</span>
      </div>
      <div class="user-info">
        <el-avatar :icon="UserFilled" size="small" />
        <span class="welcome-text">{{ userName }}님, 환영합니다.</span>
        <el-button text bg>로그아웃</el-button>
      </div>
    </el-header>

    <div class="info-wrapper">
      <!-- 좌측 (70%): 서비스 컨셉 요약 -->
      <div class="concept-banner">
        <el-icon :size="20"><CollectionTag /></el-icon>
        <div class="banner-text">
          <span>온라인 정보를 바탕으로 대기 시간을 예측하고, 나만의 웨이팅 데이터를 관리하는 서비스입니다.</span>
        </div>
      </div>
      <!-- 우측 (30%): 유저 정보 카드 플레이스홀더 -->
      <div class="user-card-placeholder">
        <UserInfoAvatar />
      </div>
    </div>

    <!-- 메인 컨텐츠 영역 -->
    <el-main class="main-content">
      <el-card class="content-card" shadow="never">
        <!-- 상단 타이틀 (프로토타입 스타일) -->
        <h1 class="main-title">맛집을 검색하여 웨이팅 데이터를 예측해보세요.</h1>

        <!-- 검색창 -->
        <el-input
            v-model="searchQuery"
            placeholder="맛집 또는 지역을 입력해 주세요"
            size="large"
            class="search-input"
            :prefix-icon="Search"
            clearable
        />

        <!-- 탭 메뉴 -->
        <el-tabs v-model="activeTab" class="content-tabs">
          <el-tab-pane
              v-for="menu in menuItems"
              :key="menu.id"
              :label="menu.label"
              :name="menu.name"
          >
            <!-- 탭 컨텐츠 (기능 설명 중심) -->
            <div class="tab-content-placeholder">
              <el-icon :size="32" class="placeholder-icon">
                <component :is="menu.icon" />
              </el-icon>
              <p>{{ menu.description }}</p>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </el-main>

    <TheFooter />
  </div>
</template>

<style scoped>
/* 이전 스타일에서 간격 위주로 수정 */
.page-container {
  height: 100vh;
  max-height: 780px;
  display: flex;
  flex-direction: column;
}

.main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 4%;
  flex-shrink: 0;
  position: relative; /* 기준점 설정을 위해 추가 */
  /* border-bottom 속성은 제거됨 */
}

/* 이 코드를 새로 추가하세요 */
.main-header::after {
  content: '';
  position: absolute;
  left: 4%;   /* 좌측 여백 */
  right: 4%;  /* 우측 여백 */
  bottom: 0;
  height: 1px; /* 선의 두께 */
  background-color: var(--el-border-color-light, #e4e7ed);
}

.logo { display: flex; align-items: center; gap: 8px; }
.logo-text { font-size: 1.2rem; font-weight: 600; color: var(--el-text-color-primary, #303133); }
.user-info { display: flex; align-items: center; gap: 12px; }
.welcome-text { font-size: 0.9rem; color: var(--el-text-color-regular); }
.user-info .el-button--text { color: var(--el-text-color-secondary); }
.user-info .el-button--text:hover { color: var(--el-color-primary); }

.main-content {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-grow: 1;
  padding: 0 1rem 1rem;
  overflow: hidden;
}

.content-card {
  width: 100%;
  max-width: 800px; /* <<<--- 최대 너비 살짝 줄임 */
  height: 500px;
  max-height: 500px;
  padding: 2rem; /* <<<--- 카드 내부 상하 패딩 감소 */
  border-radius: 4px;
  text-align: center;
}

.main-title {
  font-size: 1.7rem; /* <<<--- 폰트 크기 살짝 줄임 */
  font-weight: 600;
  color: var(--el-text-color-primary, #303133);
  margin-bottom: 1.5rem; /* <<<--- 제목 하단 마진 감소 */
}

.search-input {
  max-width: 800px; /* <<<--- 검색창 너비 살짝 줄임 */
  margin: 0 auto 2rem auto; /* <<<--- 검색창 하단 마진 감소 */
  --el-input-border-radius: 4px;
  --el-input-bg-color: var(--el-fill-color-light);
  --el-input-border-color: transparent;
}

.search-input:hover,
.search-input .is-focus {
  --el-input-border-color: var(--el-color-primary);
}

/* 탭 스타일 */
.content-tabs :deep(.el-tabs__header) { margin-bottom: 20px; } /* <<<--- 탭 헤더 하단 마진 감소 */
.content-tabs :deep(.el-tabs__nav-wrap) { justify-content: center; }
.content-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.content-tabs :deep(.el-tabs__item) { color: var(--el-text-color-secondary); font-size: 1rem; padding: 0 24px; height: 48px; }
.content-tabs :deep(.el-tabs__item.is-active) { color: var(--el-color-primary); font-weight: 600; }
.content-tabs :deep(.el-tabs__active-bar) { background-color: var(--el-color-primary); }

.tab-content-placeholder {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 12px;
  color: var(--el-text-color-secondary);
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
}

.placeholder-icon {
  color: var(--el-text-color-placeholder);
}
.info-wrapper {
  display: flex;
  gap: 20px; /* 좌우 컴포넌트 사이 간격 */
  padding: 16px 4% 0; /* 상하, 좌우 패딩 */
  flex-shrink: 0;
  margin-bottom: 0;
  height: 104px;
}
.concept-banner {
  width: 70%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background-color: #ffffff;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-light);
  height: 65px;
}

.concept-banner .el-icon {
  color: var(--el-color-primary);
  flex-shrink: 0; /* 아이콘이 찌그러지지 않도록 */
}

.concept-banner .banner-text {
  font-size: 0.9rem;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.user-card-placeholder {
  width: 30%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 0.9rem;
  color: var(--el-text-color-placeholder);
}
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