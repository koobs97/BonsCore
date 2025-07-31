<script setup>
import { ref, shallowRef, onMounted } from 'vue';
// Element Plus 아이콘 추가 (관리자 메뉴용)
import { Search, Clock, ChatDotRound, Odometer, Star, Moon, Sunny, Setting, User, CollectionTag } from '@element-plus/icons-vue';
import TheFooter from "@/components/layout/TheFooter.vue";
import UserInfoAvatar from "@/components/login/userInfoAvatar.vue";
import { userStore } from '@/store/userStore';
import { useRouter } from 'vue-router';

// ------------------- [신규] 관리자 사이드바 컴포넌트 import -------------------
import AdminSidebar from "@/components/layout/AdminSidebar.vue"; // 경로는 실제 파일 위치에 맞게 수정하세요.

// 로그인한 사용자 이름 (임시 데이터)
const userName = ref('');

// router, user
const router = useRouter();
const userStoreObj = userStore();

// ------------------- [신규] 관리자 상태 및 메뉴 데이터 -------------------
const isAdmin = ref(false); // 관리자 여부 상태

// 관리자용 메뉴 데이터 (DB에서 조회했다고 가정)
const adminMenuItems = shallowRef([
  {
    id: 101,
    name: '시스템 관리',
    icon: Setting,
    children: [
      { id: 1011, name: '사용자 관리', url: '/admin/users', icon: User },
      { id: 1012, name: '메뉴 관리', url: '/admin/menus', icon: CollectionTag },
    ]
  },
  {
    id: 102,
    name: '데이터 통계',
    icon: Odometer,
    children: [
      { id: 1021, name: '시간대별 통계', url: '/admin/stats/hourly', icon: Clock },
      { id: 1022, name: '사용자 활동 로그', url: '/admin/logs/activity', icon: Search },
    ]
  },
]);


// 검색어 데이터
const searchQuery = ref('');

// 활성화된 탭 관리
const activeTab = ref('prediction');

// 일반 사용자용 메뉴 데이터 (기존과 동일)
const menuItems = shallowRef([
  { id: 1, name: 'prediction', label: '시간대별 예측', icon: Clock, description: '축적된 데이터를 기반으로 시간대별 대기 시간을 예측합니다.' },
  { id: 2, name: 'community', label: '저장소', icon: ChatDotRound, description: '나만의 데이터 저장' },
]);

onMounted(async () => {
  const isLoggedIn = userStore().isLoggedIn;
  if (!isLoggedIn) {
    await router.push("login");
  } else {
    const user = userStoreObj.getUserInfo;
    userName.value = user.userName;
    isAdmin.value = true;
    // // ------------------- [수정] 사용자 역할 확인 로직 -------------------
    // // userStore에 roles 배열이 있고, 그 안에 'ROLE_ADMIN'이 포함되어 있는지 확인
    // // 실제 userStore의 구조에 따라 이 부분은 달라질 수 있습니다.
    // if (user.roles && user.roles.includes('ROLE_ADMIN')) {
    //   isAdmin.value = true;
    // }
  }
});

// isDarkMode, toggleTheme 함수가 없어서 임시로 추가합니다. (기존 코드에 있다면 이 부분은 무시하세요)
const isDarkMode = ref(false);
const toggleTheme = () => { isDarkMode.value = !isDarkMode.value; };

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

    <!-- [수정] 관리자/일반사용자 레이아웃 분기를 위한 wrapper -->
    <div class="content-layout-wrapper">

      <!-- [신규] 관리자일 경우에만 사이드바 표시 -->
      <AdminSidebar v-if="isAdmin" :menu-items="adminMenuItems" />

      <!-- 기존 컨텐츠 영역을 div로 한번 더 감싸기 (레이아웃 조정을 위해) -->
      <div class="main-content-area">
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
      </div>

    </div>

    <TheFooter />
  </div>
</template>

<style scoped>
/* ------------ [신규] 레이아웃 관련 스타일 추가 ----------- */
.content-layout-wrapper {
  display: flex;
  width: 100%;
  max-width: 1200px; /* 관리자 모드일 때 전체 너비를 넓힘 */
  justify-content: center;
  gap: 24px; /* 사이드바와 메인 컨텐츠 사이의 간격 */
  flex-grow: 1;
  overflow: hidden;
}

.main-content-area {
  display: flex;
  flex-direction: column;
}
/* ---------------------------------------------------- */


/* 이전 스타일에서 간격 위주로 수정 */
.page-container {
  height: 100vh;
  max-height: 780px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.main-header {
  width: 100%;
  max-width: 1200px; /* 관리자 모드일 때 헤더 너비도 같이 넓힘 */
  height: 50px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 12px;
  box-sizing: border-box;
  flex-shrink: 0;
}

/* 헤더 밑줄 관련 스타일은 기존 것을 유지해도 좋습니다 */
/*.main-header::after { ... }*/

.info-wrapper {
  display: flex;
  gap: 12px;
  width: 800px;
  height: 93px;
  flex-shrink: 0;
  margin-top: 12px;
  margin-bottom: 12px;
}
.logo { display: flex; align-items: center; gap: 8px; }
.logo-text { font-size: 19px; font-weight: 600; color: #303133; }

.main-content {
  display: flex;
  justify-content: center;
  width: 820px;
  align-items: flex-start;
  flex-grow: 1;
  overflow: hidden;
  padding: 0;
}

.content-card {
  width: 820px;
  height: 570px;
  max-height: 570px;
  border-radius: 4px;
  text-align: center;
  padding: 24px;
  box-sizing: border-box;
}

/* 이하 기존 스타일은 그대로 유지 */
.search-input {
  max-width: 800px;
  margin: 0 auto 2rem auto;
  --el-input-border-radius: 4px;
  --el-input-bg-color: var(--el-fill-color-light);
  --el-input-border-color: transparent;
}
.search-input:hover,
.search-input .is-focus {
  --el-input-border-color: var(--el-color-primary);
}
.content-tabs :deep(.el-tabs__header) { margin-bottom: 20px; }
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
  padding: 8px;
  box-sizing: border-box;
}
.concept-banner .el-icon {
  color: var(--el-color-primary);
  flex-shrink: 0;
  padding-left: 8px;
}
.concept-banner .banner-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}
.user-card-placeholder {
  width: 30%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 14px;
  color: #c0c4cc;
}
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