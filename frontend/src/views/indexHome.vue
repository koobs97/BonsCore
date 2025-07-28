<script setup>
import { ref, shallowRef, onMounted } from 'vue';
import { Search, Clock, ChatDotRound, Odometer, Star, Moon, Sunny } from '@element-plus/icons-vue';
import TheFooter from "@/components/layout/TheFooter.vue";
import UserInfoAvatar from "@/components/login/userInfoAvatar.vue";
import { userStore } from '@/store/userStore';
import { useRouter } from 'vue-router';

// 로그인한 사용자 이름 (임시 데이터)
const userName = ref('');

// router, user
const router = useRouter();
const userStoreObj = userStore();

// 검색어 데이터
const searchQuery = ref('');

// 활성화된 탭 관리
const activeTab = ref('prediction');

// DB에서 조회했다고 가정한 임시 메뉴 데이터 (프로토타입에 맞게 설명 수정)
const menuItems = shallowRef([
  { id: 1, name: 'prediction', label: '시간대별 예측', icon: Clock, description: '축적된 데이터를 기반으로 시간대별 대기 시간을 예측합니다.' },
  { id: 2, name: 'community', label: '저장소', icon: ChatDotRound, description: '나만의 데이터 저장' },
]);

onMounted(async () => {
  const isLoggedIn = userStore().isLoggedIn;
  if (!isLoggedIn) {
    await router.push("login");
  }
  else {
    const user = userStoreObj.getUserInfo;
    userName.value = user.userName;
  }
})
</script>

<template>
  <div class="page-container">
    <!-- 상단 네비게이션 바 (로그인 상태) -->
    <el-header class="main-header">
      <div class="logo">
        <el-icon :size="24" color="#001233" class="logo-icon"><Odometer /></el-icon>
        <span class="logo-text">웨이팅 레이더</span>
      </div>
      <div class="header-actions">
        <el-tooltip content="최근 검색 기록" placement="bottom">
          <el-button :icon="Clock" circle />
        </el-tooltip>
        <el-tooltip content="즐겨찾기" placement="bottom">
          <el-button :icon="Star" circle />
        </el-tooltip>
        <el-tooltip content="테마 변경" placement="bottom">
          <el-button :icon="isDarkMode ? Moon : Sunny" circle @click="toggleTheme" />
        </el-tooltip>
      </div>
    </el-header>

    <div class="info-wrapper">
      <!-- 좌측 (70%): 서비스 컨셉 요약 -->
      <div class="concept-banner">
        <el-icon :size="20"><CollectionTag /></el-icon>
        <div class="banner-text">
          <span>온라인 정보를 바탕으로 대기 시간을 예측. 나만의 웨이팅 데이터를 관리.</span>
        </div>
      </div>
      <!-- 우측 (30%): 유저 정보 카드 플레이스홀더 -->
      <UserInfoAvatar />
    </div>

    <!-- 메인 컨텐츠 영역 -->
    <el-main class="main-content">
      <el-card class="content-card" shadow="never">

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
  align-items: center;
}

.main-header {
  width: 800px; /* 헤더는 전체 너비를 사용 */
  height: 50px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #ffffff;
  /* 복잡한 ::after 선택자 대신 명확한 border-bottom으로 변경 */
  border-bottom: 1px solid #e4e7ed;
  /* 헤더 내용이 좌우 끝에 붙지 않도록 내부 여백(padding)을 추가 */
  padding: 0 12px;
  box-sizing: border-box; /* padding이 너비/높이에 포함되도록 설정 */
  flex-shrink: 0;
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
.info-wrapper {
  display: flex;
  gap: 12px;
  width: 800px;
  height: 93px;
  flex-shrink: 0;
  margin-top: 12px; /* 헤더와의 간격 */
  margin-bottom: 12px; /* 메인 콘텐츠와의 간격 */
}
.logo { display: flex; align-items: center; gap: 8px; }
/* rem 단위를 px로 변환 (1.2rem -> 19px) */
.logo-text { font-size: 19px; font-weight: 600; color: #303133; }

.main-content {
  display: flex;
  justify-content: center;
  width: 820px;
  align-items: flex-start; /* 콘텐츠를 위에서부터 정렬하도록 변경 */
  flex-grow: 1;
  overflow: hidden;
}

main.el-main.main-content {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  flex-grow: 1;
  overflow: hidden;
  width: 800px;
  padding: 0;
}

.content-card {
  width: 820px;
  height: 570px;
  max-height: 570px;
  border-radius: 4px;
  text-align: center;
  padding: 24px; /* 카드 내부에 여백을 주어 답답함을 해소 */
  box-sizing: border-box;
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
.content-tabs :deep(.el-tabs__item) { color: var(--el-text-color-secondary); font-size: 1rem; height: 48px; }
.content-tabs :deep(.el-tabs__item.is-active) { color: var(--el-color-primary); font-weight: 600; }
.content-tabs :deep(.el-tabs__active-bar) { background-color: var(--el-color-primary); }

.tab-content-placeholder {
  height: 350px;
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
.concept-banner {
  width: 70%;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  background-color: #ffffff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  padding: 8px; /* 요청에 따라 기존 padding 값 유지 */
  box-sizing: border-box;
}

.concept-banner .el-icon {
  color: var(--el-color-primary);
  flex-shrink: 0; /* 아이콘이 찌그러지지 않도록 */
  padding-left: 8px;
}

.concept-banner .banner-text {
  font-size: 14px; /* rem 단위를 px로 변환 */
  color: #606266;
  line-height: 1.6;
}

.user-card-placeholder {
  width: 30%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 14px; /* rem 단위를 px로 변환 */
  color: #c0c4cc;
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