<script setup>
import { ref, shallowRef, onMounted, defineAsyncComponent, computed } from 'vue';
// [수정] ArrowDown 아이콘 추가
import {
  Search, Clock, ChatDotRound, Odometer, Moon, Sunny,
  Setting, User, CollectionTag, Tools, Operation, ChatSquare, Box, ArrowDown,
} from '@element-plus/icons-vue';
import TheFooter from "@/components/layout/TheFooter.vue";
import UserInfoAvatar from "@/components/login/userInfoAvatar.vue";
import { userStore } from '@/store/userStore';
import { useRouter } from 'vue-router';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import TimeViewr from "@/components/time/TimeViewr.vue";

// router, user
const router = useRouter();
const userStoreObj = userStore();

// ------------------- 상태(State) 관리 -------------------
const isLoading = ref(true);
const userName = ref('');
const isAdmin = ref(false);
const searchQuery = ref('');
const activeMainTab = ref('user');
// [수정] activeUserTab을 activeUserMenuUrl로 변경하여 현재 선택된 메뉴의 URL을 추적합니다.
const activeUserMenuUrl = ref('');
const activeAdminMenu = ref('');

// ------------------- 메뉴 데이터 -------------------
const userMenuItems = shallowRef([]);
const adminMenuItems = shallowRef([]);

// 아이콘 매핑 (기존과 동일)
const iconMap = {
  '시간대별 예측': Clock,
  '저장소': ChatDotRound,
  '시스템 관리': Setting,
  '사용자 관리': User,
  '메뉴 관리': CollectionTag,
  '활동 로그': Search,
  '데이터 통계': Odometer,
  '기본아이콘': Operation
};

// 메뉴 트리 변환 함수 (기존과 동일)
const buildMenuTree = (flatMenus, parentId = null) => {
  return flatMenus
      .filter(menu => menu.parentMenuId === parentId)
      .sort((a, b) => Number(a.sortOrder) - Number(b.sortOrder))
      .map(menu => {
        const children = buildMenuTree(flatMenus, menu.menuId);
        return {
          id: menu.menuId,
          name: menu.menuName,
          label: menu.menuName,
          url: menu.menuUrl || '',
          icon: iconMap[menu.menuName] || iconMap['기본아이콘'],
          description: `메뉴: ${menu.menuName}`,
          children: children.length > 0 ? children : undefined,
        };
      });
};

// 관리자 컴포넌트 맵 생성 (기존과 동일)
const adminComponentFiles = import.meta.glob('@/views/admin/*.vue');
const adminComponentMap = Object.keys(adminComponentFiles).reduce((map, path) => {
  const componentName = path.split('/').pop().replace('.vue', '');
  map[componentName] = defineAsyncComponent(adminComponentFiles[path]);
  return map;
}, {});

// 사용자 서비스 컴포넌트 맵 생성 (기존과 동일)
const userComponentFiles = import.meta.glob('@/views/biz/*.vue');
const userComponentMap = Object.keys(userComponentFiles).reduce((map, path) => {
  const componentName = path.split('/').pop().replace('.vue', '');
  map[componentName] = defineAsyncComponent(userComponentFiles[path]);
  return map;
}, {});

// ======================= [신규] 동적 컴포넌트 로딩 로직 =======================
// 현재 활성화된 관리자 메뉴에 따른 컴포넌트 반환 (기존과 동일)
const currentAdminComponent = computed(() => {
  return adminComponentMap[activeAdminMenu.value] || null;
});

// [신규] 현재 활성화된 사용자 메뉴 URL에 따라 렌더링할 컴포넌트를 반환합니다.
const currentUserComponent = computed(() => {
  if (!activeUserMenuUrl.value) return null; // 선택된 메뉴가 없으면 null 반환
  return userComponentMap[activeUserMenuUrl.value] || null;
});
// =========================================================================

onMounted(async () => {
  isLoading.value = true;
  try {
    const isLoggedIn = userStore().isLoggedIn;
    if (!isLoggedIn) {
      await router.push("/login");
      return;
    }

    const user = userStoreObj.getUserInfo;
    userName.value = user.userName;

    const response = await Api.post(ApiUrls.GET_MENUS, { userId: user.userId });
    const allMenus = response.data || [];
    const menuTree = buildMenuTree(allMenus);

    const adminRootMenus = menuTree.filter(menu => menu.name === '시스템 관리');
    const regularUserMenus = menuTree.filter(menu => menu.name !== '시스템 관리');

    if (user.roleId === 'ADMIN') {
      isAdmin.value = true;
      adminMenuItems.value = adminRootMenus;
      if (adminRootMenus[0]?.children?.[0]?.url) {
        activeAdminMenu.value = adminRootMenus[0].children[0].url;
      }
    }

    userMenuItems.value = regularUserMenus;
    // [수정] 컴포넌트 첫 진입 시, 첫 번째 사용자 메뉴를 활성화합니다.
    if (regularUserMenus.length > 0 && regularUserMenus[0].url) {
      activeUserMenuUrl.value = regularUserMenus[0].url;
    }

  } catch (error) {
    console.error("메뉴 정보를 가져오는 데 실패했습니다:", error);
  } finally {
    isLoading.value = false;
  }
});


// [신규] 사용자 메뉴 선택 핸들러
const handleUserMenuSelect = (url) => {
  if (url) {
    activeUserMenuUrl.value = url;
  }
};

// 핸들러 및 기타 함수 (기존과 동일)
const handleAdminMenuSelect = (index) => {
  activeAdminMenu.value = index;
};
const isDarkMode = ref(false);
const toggleTheme = () => {
  isDarkMode.value = !isDarkMode.value;
};
const goToGitHub = () => {
  window.open('https://github.com/koobs97/BonsCore/tree/main', '_blank');
}
</script>

<template>
  <div class="page-container">
    <!-- 상단 네비게이션 바 (기존과 동일) -->
    <el-header class="main-header">
      <div style="display: flex; align-items: center; justify-content: center; gap: 4px; padding: 8px 16px 8px 16px; background-color: #f4f7f9; border-radius: 4px;">
        <span style="font-family: 'Poppins', sans-serif; font-size: 1rem; font-weight: 400; color: #777;">Project By</span>
        <span style="font-family: 'Poppins', sans-serif; font-size: 1rem; font-weight: 700; color: #333;">Bons</span>
        <div style="width: 7px; height: 7px; background-color: var(--el-color-primary); border-radius: 50%;"></div>
      </div>
      <div class="header-actions">
        <el-button class="custom-image-button" :icon="ChatSquare" />
        <el-tooltip content="테마 변경" placement="bottom">
          <el-button class="custom-image-button" :icon="isDarkMode ? Moon : Sunny" @click="toggleTheme"/>
        </el-tooltip>
        <el-tooltip content="깃허브 바로가기" placement="bottom">
          <el-button class="custom-image-button" @click="goToGitHub">
            <img src="@/assets/images/github_icon.png" alt="custom icon"/>
          </el-button>
        </el-tooltip>
      </div>
    </el-header>

    <div class="content-layout-wrapper">
      <div class="main-content-area">
        <!-- 정보 래퍼 (기존과 동일) -->
        <div class="info-wrapper">
<!--            <img src="@/assets/images/spring-icon.svg" class="banner-icon" alt="Spring Boot Icon">-->
<!--            <div class="banner-text">-->
<!--              <span class="banner-title">스프링부트 기반 개인 프로젝트</span>-->
<!--            </div>-->

            <div style="display: flex; align-items: center; gap: 20px; padding: 24px; border-radius: 4px;
background:
linear-gradient(135deg, rgba(74, 144, 226, 0.15) 0%, rgba(255, 255, 255, 0.9) 70%), /* 은은한 그라데이션 */
linear-gradient(to bottom, rgba(74, 144, 226, 0.15) 1px, transparent 1px), /* 가로선 */
linear-gradient(to right, rgba(74, 144, 226, 0.2) 1px, transparent 1px); /* 세로선 */
background-size: 100%, 20px 20px, 20px 20px; /* 그리드 크기 지정 */
border: 1px solid #e2dffc;
box-shadow: 0 4px 12px rgba(108, 92, 231, 0.05); width: 64%;">
              <div style="width: 52px; height: 52px; border-radius: 50%; background-color: #eef2f7; display: flex; align-items: center; justify-content: center;">
                <img src="@/assets/images/spring-icon.svg" class="banner-icon" alt="Spring Boot Icon">
              </div>
              <div style="display: flex; flex-direction: column; gap: 4px;">
                <h3 style="margin: 0; font-family: 'Poppins', sans-serif; font-size: 1.1rem; font-weight: 600; color: #333;">Spring Boot Project</h3>
                <p style="margin: 0; font-family: 'Noto Sans KR', sans-serif; font-size: 0.85rem; color: #777; text-align: left;">Waiting Index Analyzer</p>
              </div>
            </div>


          <UserInfoAvatar/>
        </div>

        <el-main class="main-content">
          <el-card class="content-card" shadow="never">
            <el-skeleton :rows="10" animated v-if="isLoading"/>
            <el-tabs v-model="activeMainTab" class="main-mode-tabs" v-else>

              <!-- ======================= [수정된 사용자 탭] ======================= -->
              <el-tab-pane name="user">
                <!-- 1. #label 슬롯을 사용하여 탭의 제목을 <el-dropdown>으로 교체 -->
                <template #label>
                  <el-dropdown trigger="hover" @command="handleUserMenuSelect">
                    <span class="el-dropdown-link">
                      서비스
                      <el-icon class="el-icon--right"><arrow-down/></el-icon>
                    </span>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item
                            v-for="menu in userMenuItems"
                            :key="menu.id"
                            :command="menu.url"
                            :icon="menu.icon"
                        >
                          {{ menu.name }}
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </template>

                <!-- 2. 사용자 탭 컨텐츠 영역 -->
                <div v-if="userMenuItems.length > 0">
                  <!-- 3. 선택된 메뉴에 따라 동적으로 컴포넌트를 렌더링 -->
                  <div class="user-content-container">
                    <keep-alive>
                      <component :is="currentUserComponent" v-if="currentUserComponent"/>
                    </keep-alive>

                    <!-- 4. 컴포넌트를 찾지 못했을 경우의 폴백 UI -->
                    <div v-if="!currentUserComponent && activeUserMenuUrl" class="tab-content-placeholder">
                      <el-icon :size="32" class="placeholder-icon">
                        <Tools/>
                      </el-icon>
                      <p>'{{ activeUserMenuUrl }}' 컴포넌트를 찾을 수 없습니다.</p>
                      <p style="font-size: 12px; color: #909399;">/src/views/biz/{{ activeUserMenuUrl }}.vue 파일이 있는지
                        확인해주세요.</p>
                    </div>
                  </div>
                </div>

                <!-- 사용 가능한 서비스 메뉴가 없을 경우 -->
                <div v-else class="tab-content-placeholder">
                  <p>사용 가능한 서비스 메뉴가 없습니다.</p>
                </div>
              </el-tab-pane>
              <!-- ================================================================ -->


              <!-- 관리자 탭 (기존과 동일) -->
              <el-tab-pane v-if="isAdmin" name="admin">
                <template #label>
                  <div class="admin-tab-label">
                    <span>관리자</span>
                  </div>
                </template>

                <div class="admin-panel-layout">
                  <div class="admin-menu-container">
                    <el-menu
                        v-if="adminMenuItems.length > 0"
                        :default-active="activeAdminMenu"
                        @select="handleAdminMenuSelect"
                        class="admin-menu"
                        mode="horizontal"
                    >
                      <el-sub-menu v-for="menu in adminMenuItems" :key="menu.id" :index="String(menu.id)">
                        <template #title>
                          <el-icon>
                            <component :is="menu.icon"/>
                          </el-icon>
                          <span>{{ menu.name }}</span>
                        </template>
                        <el-menu-item v-for="child in menu.children" :key="child.url" :index="child.url">
                          <el-icon>
                            <component :is="child.icon"/>
                          </el-icon>
                          <span>{{ child.name }}</span>
                        </el-menu-item>
                      </el-sub-menu>
                    </el-menu>
                  </div>

                  <div class="admin-content-container">
                    <component :is="currentAdminComponent" v-if="currentAdminComponent"/>
                    <div v-else>
                      <p>관리자 메뉴를 선택해주세요.</p>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </el-card>
        </el-main>
      </div>
    </div>
    <TheFooter/>
  </div>
</template>

<style scoped>
@import url("https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css");
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;700&family=Poppins:wght@600;700&family=Outfit:wght@600;700&family=Rubik:wght@600;700&display=swap');
/* 모든 스타일은 변경 없이 그대로 유지됩니다 */
.page-container {
  height: 120vh;
  max-height: 880px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.main-header {
  width: 100%;
  max-width: 900px;
  height: 50px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0;
  box-sizing: border-box;
  flex-shrink: 0;
}

.custom-image-button {
  padding: 0;
  width: 32px;
  height: 32px;
}

.custom-image-button img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: block;
}

.content-layout-wrapper {
  display: flex;
  justify-content: center;
  width: 100%;
  flex-grow: 1;
  overflow: hidden;
}

.main-content-area {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.info-wrapper {
  display: flex;
  gap: 12px;
  width: 820px;
  height: 95px;
  flex-shrink: 0;
  margin-top: 12px;
  margin-bottom: 12px;
}

.main-content {
  width: 820px;
  padding: 0;
}

.content-card {
  width: 100%;
  height: 680px;
  min-height: 600px;
  border-radius: 4px;
  padding: 8px;
  box-sizing: border-box;
  box-shadow: 0 4px 12px rgba(108, 92, 231, 0.05);
}

/* [신규/수정] 사용자 컨텐츠 영역 스타일 추가 */
.user-content-container {
  height: 600px; /* 관리자 패널과 높이를 맞추거나 조절 */
  overflow-y: auto;
  padding: 0;
  margin: 0; /* 부모 패딩 고려 */
}

/* [신규] 드롭다운 링크 스타일 */
.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
  color: var(--el-color-primary); /* Element Plus 테마 색상 사용 */
  font-size: 1rem; /* [수정] 탭과 동일하게 1rem으로 명시적으로 지정 */
  font-weight: 500; /* [추가] 탭 기본 폰트 두께와 맞춤 */
  outline: none; /* 클릭 시 테두리 제거 */
}

.el-dropdown-link:hover {
  color: var(--el-color-primary-light-3);
}

.admin-tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.admin-panel-layout {
  display: flex;
  flex-direction: column;
  height: 550px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
}

.admin-menu-container {
  flex-shrink: 0;
  border-bottom: 1px solid var(--el-border-color-light);
  overflow-y: visible;
}

.admin-menu.el-menu--horizontal {
  border-bottom: none !important;
}

.admin-content-container {
  flex-grow: 1;
  padding: 8px;
  overflow-y: auto;
}

.main-mode-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.main-mode-tabs :deep(.el-tabs__item) {
  font-size: 1rem; /* [수정] 폰트 크기를 1rem으로 조정 */
  height: 48px;
  /* 드롭다운이 탭 내부에 있으므로, 패딩을 조절하여 정렬을 맞춥니다. */
  padding: 0 16px;
}

/* 드롭다운이 포함된 탭의 padding을 0으로 설정하여 자체 스타일링을 쉽게 합니다. */
.main-mode-tabs :deep(.el-tabs__item:has(.el-dropdown)) {
  padding: 0;
}


.search-input {
  margin-bottom: 1rem;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
}
.logo-text {
  font-size: 1.8rem;
  margin: 0;
  font-weight: 700;
  color: #303133;
  font-family: 'Outfit', sans-serif;
}

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
  align-items: center; /* 세로 중앙 정렬 */
  gap: 12px; /* 아이콘과 텍스트 사이 간격 */
  background-color: #ffffff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  padding: 0 28px; /* 내부 좌우 여백 */
  box-sizing: border-box;
}

.concept-banner .el-icon {
  color: var(--el-color-primary);
}

.concept-banner .banner-text {
  font-size: 14px;
  color: #606266;
}
.banner-icon {
  width: 24px;  /* 아이콘 크기 지정 */
  height: 24px; /* 아이콘 크기 지정 */
}

/* 2. 텍스트 강조 스타일 */
.banner-title {
  font-size: 15px; /* 기본 폰트보다 살짝 크게 */
  font-weight: 500; /* 살짝 두껍게 */
  color: #606266;
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