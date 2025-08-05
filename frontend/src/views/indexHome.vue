<script setup>
import { ref, shallowRef, onMounted, defineAsyncComponent, computed } from 'vue';
// 아이콘 import (기존과 동일)
import {
  Search, Clock, ChatDotRound, Odometer, Star, Moon, Sunny,
  Setting, User, CollectionTag, Tools, Operation, ChatSquare,
} from '@element-plus/icons-vue';
import TheFooter from "@/components/layout/TheFooter.vue";
import UserInfoAvatar from "@/components/login/userInfoAvatar.vue";
import { userStore } from '@/store/userStore';
import { useRouter } from 'vue-router';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";

// router, user
const router = useRouter();
const userStoreObj = userStore();

// ------------------- 상태(State) 관리 -------------------
const isLoading = ref(true); // [신규] 로딩 상태 추가
const userName = ref('');
const isAdmin = ref(false);
const searchQuery = ref('');
const activeMainTab = ref('user');
const activeUserTab = ref(''); // [변경] 초기값을 빈 문자열로 설정
const activeAdminMenu = ref('');

// ------------------- 메뉴 데이터 -------------------
const userMenuItems = shallowRef([]);
const adminMenuItems = shallowRef([]);

// [변경 없음] 아이콘 매핑
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

// [변경] 더 안전하고 단순화된 메뉴 트리 변환 함수
const buildMenuTree = (flatMenus, parentId = null) => {
  return flatMenus
      .filter(menu => menu.parentMenuId === parentId)
      .sort((a, b) => Number(a.sortOrder) - Number(b.sortOrder)) // sortOrder를 숫자로 명시적 변환
      .map(menu => {
        const children = buildMenuTree(flatMenus, menu.menuId);
        return {
          // Vue 템플릿에서 사용할 속성들을 명확하게 정의
          id: menu.menuId,
          name: menu.menuName,
          label: menu.menuName,
          url: menu.menuUrl || '', // null 대신 빈 문자열
          icon: iconMap[menu.menuName] || iconMap['기본아이콘'],
          description: `메뉴: ${menu.menuName}`,
          children: children.length > 0 ? children : undefined,
        };
      });
};

// 1. Vite의 glob 기능을 사용해 /src/views/admin 폴더 아래의 모든 .vue 파일을 가져옵니다.
//    이 코드는 컴파일 시점에 해당 폴더의 파일 목록을 기반으로 맵을 생성합니다.
const adminComponentFiles = import.meta.glob('@/views/admin/*.vue');

// 2. 위에서 가져온 파일 목록을 우리가 사용하기 편한 형태로 가공합니다.
//    {'Log': () => import('./views/admin/Log.vue'), ...} 와 같은 객체로 만듭니다.
const adminComponentMap = Object.keys(adminComponentFiles).reduce((map, path) => {
  // 파일 경로에서 파일 이름(확장자 제외)만 추출합니다.
  // 예: '@/views/admin/UserManagement.vue' -> 'UserManagement'
  const componentName = path.split('/').pop().replace('.vue', '');

  // 맵에 { '컴포넌트이름': 동적 import 함수 } 형태로 저장합니다.
  // defineAsyncComponent로 감싸서 비동기 컴포넌트로 만듭니다.
  map[componentName] = defineAsyncComponent(adminComponentFiles[path]);

  return map;
}, {});

// 3. 현재 활성화된 메뉴 키에 따라 맵에서 올바른 컴포넌트를 찾아 반환합니다.
const currentAdminComponent = computed(() => {
  return adminComponentMap[activeAdminMenu.value] || null;
});

onMounted(async () => {
  isLoading.value = true; // API 호출 시작 시 로딩 상태로 설정
  try {
    const isLoggedIn = userStore().isLoggedIn;
    if (!isLoggedIn) {
      await router.push("/login");
      return; // 로그인 페이지로 갔으므로 더 이상 진행하지 않음
    }

    const user = userStoreObj.getUserInfo;
    userName.value = user.userName;

    // API 호출로 메뉴 데이터 가져오기
    const response = await Api.post(ApiUrls.GET_MENUS, { userId: user.userId });
    const allMenus = response.data || []; // 데이터가 없을 경우 빈 배열로 처리

    // 플랫 데이터를 계층 구조로 변환
    const menuTree = buildMenuTree(allMenus);

    // [변경] 관리자 메뉴와 일반 사용자 메뉴를 더 유연하게 분리
    // '시스템 관리' 메뉴가 최상위 메뉴에 있는지 여부로 관리자 판단
    const adminRootMenus = menuTree.filter(menu => menu.name === '시스템 관리'); // 예시 기준
    const regularUserMenus = menuTree.filter(menu => menu.name !== '시스템 관리');

    if (user.roleId === 'ADMIN') {
      isAdmin.value = true;
      adminMenuItems.value = adminRootMenus;
      // 관리자의 첫 번째 하위 메뉴를 기본 활성화 메뉴로 설정
      if (adminRootMenus[0]?.children?.[0]?.url) {
        activeAdminMenu.value = adminRootMenus[0].children[0].url;
      }
    }

    userMenuItems.value = regularUserMenus;
    // 일반 사용자의 첫 번째 메뉴를 기본 활성화 탭으로 설정
    if (regularUserMenus.length > 0) {
      activeUserTab.value = regularUserMenus[0].id; // el-tab-pane의 name은 id로 사용
    }

  } catch (error) {
    console.error("메뉴 정보를 가져오는 데 실패했습니다:", error);
    // TODO: 사용자에게 에러 알림 표시
  } finally {
    isLoading.value = false; // API 호출 완료/실패 후 로딩 상태 해제
  }
});


// 핸들러 및 기타 함수 (변경 없음)
const handleAdminMenuSelect = (index) => {
  activeAdminMenu.value = index;
  console.log(activeAdminMenu.value)
};
const isDarkMode = ref(false);
const toggleTheme = () => { isDarkMode.value = !isDarkMode.value; };
</script>

<template>
  <div class="page-container">
    <!-- 상단 네비게이션 바 (변경 없음) -->
    <el-header class="main-header">
      <!-- ... -->
      <div class="logo">
        <el-icon :size="24" color="#001233" class="logo-icon"><Odometer /></el-icon>
        <span class="logo-text">웨이팅 레이더</span>
      </div>
      <div class="header-actions">

        <el-button :icon="ChatSquare" circle /> <!-- chatUi url 추가 -->
        <el-tooltip content="테마 변경" placement="bottom">
          <el-button :icon="isDarkMode ? Moon : Sunny" circle @click="toggleTheme" />
        </el-tooltip>
        <el-button :icon="ChatSquare" circle /> <!-- github 추가 -->

      </div>
    </el-header>

    <div class="content-layout-wrapper">
      <div class="main-content-area">
        <div class="info-wrapper">
          <div class="concept-banner">
            <el-icon :size="20"><CollectionTag /></el-icon>
            <div class="banner-text">
              <span>온라인 정보를 바탕으로 대기 시간을 예측. 나만의 웨이팅 데이터를 관리.</span>
            </div>
          </div>
          <UserInfoAvatar />
        </div>

        <el-main class="main-content">
          <el-card class="content-card" shadow="never">
            <!-- [변경] 로딩 상태에 따라 스켈레톤 또는 실제 컨텐츠 표시 -->
            <el-skeleton :rows="10" animated v-if="isLoading" />
            <el-tabs v-model="activeMainTab" class="main-mode-tabs" v-else>

              <!-- 1. 일반 사용자 탭 -->
              <el-tab-pane label="서비스" name="user">
                <el-input
                    v-model="searchQuery"
                    placeholder="맛집 또는 지역을 입력해 주세요"
                    size="large"
                    class="search-input"
                    :prefix-icon="Search"
                    clearable
                />
                <!-- [변경] userMenuItems 데이터가 있을 때만 렌더링 -->
                <el-tabs v-if="userMenuItems.length > 0" v-model="activeUserTab" class="content-tabs">
                  <el-tab-pane
                      v-for="menu in userMenuItems"
                      :key="menu.id"
                      :label="menu.label"
                      :name="menu.id"
                  >
                    <div class="tab-content-placeholder">
                      <el-icon :size="32" class="placeholder-icon">
                        <component :is="menu.icon" />
                      </el-icon>
                      <p>{{ menu.description }}</p>
                    </div>
                  </el-tab-pane>
                </el-tabs>
                <div v-else class="tab-content-placeholder">
                  <p>사용 가능한 서비스 메뉴가 없습니다.</p>
                </div>
              </el-tab-pane>

              <!-- 2. 관리자 탭 (관리자일 때만 보임) -->
              <el-tab-pane v-if="isAdmin" name="admin">
                <template #label>
                  <div class="admin-tab-label">
                    <el-icon><Tools /></el-icon>
                    <span>관리자</span>
                  </div>
                </template>

                <div class="admin-panel-layout">
                  <!-- 좌측: 관리자 메뉴 -->
                  <div class="admin-menu-container">
                    <!-- [변경] adminMenuItems 데이터가 있을 때만 렌더링 -->
                    <el-menu
                        v-if="adminMenuItems.length > 0"
                        :default-active="activeAdminMenu"
                        @select="handleAdminMenuSelect"
                        class="admin-menu"
                        mode="horizontal"
                    >
                      <!-- el-sub-menu의 index는 고유 식별자 역할을 하므로 그대로 두어도 괜찮습니다. -->
                      <el-sub-menu v-for="menu in adminMenuItems" :key="menu.id" :index="String(menu.id)">
                        <template #title>
                          <el-icon><component :is="menu.icon" /></el-icon>
                          <span>{{ menu.name }}</span>
                        </template>
                        <el-menu-item v-for="child in menu.children" :key="child.url" :index="child.url">
                          <el-icon><component :is="child.icon" /></el-icon>
                          <span>{{ child.name }}</span>
                        </el-menu-item>
                      </el-sub-menu>
                    </el-menu>
                  </div>

                  <!-- 우측: 선택된 메뉴에 따른 컨텐츠 -->
                  <div class="admin-content-container">
                    <!-- 이 코드는 script의 로직이 아무리 복잡해져도 그대로 유지됩니다 -->
                    <component :is="currentAdminComponent" v-if="currentAdminComponent" />
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
    <TheFooter />
  </div>
</template>

<style scoped>
/* CSS는 변경할 필요가 없습니다. */
/* ... 기존 스타일 그대로 ... */

.page-container {
  height: 100vh;
  max-height: 800px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.main-header {
  width: 100%;
  max-width: 900px; /* 컨텐츠 카드 너비에 맞게 조정 */
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

.content-layout-wrapper {
  display: flex;
  justify-content: center;
  width: 100%;
  flex-grow: 1;
  overflow: hidden; /* 스크롤은 내부에서 처리 */
}

.main-content-area {
  display: flex;
  flex-direction: column;
  align-items: center; /* 자식 요소들을 가운데 정렬 */
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
  min-height: 570px; /* 최소 높이 설정, 컨텐츠에 따라 늘어날 수 있음 */
  border-radius: 4px;
  padding: 12px 24px 24px 24px; /* 위쪽 패딩 줄임 */
  box-sizing: border-box;
}

.admin-tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.admin-panel-layout {
  display: flex;
  flex-direction: column; /* 가로(row) -> 세로(column)로 변경 */
  height: 480px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
}

/* [수정] admin-menu-container: 가로 메뉴에 맞게 스타일 조정 */
.admin-menu-container {
  /* width: 200px; */ /* 더 이상 고정 너비가 필요 없음 */
  flex-shrink: 0;
  /* border-right를 border-bottom으로 변경하여 메뉴와 컨텐츠를 구분 */
  border-bottom: 1px solid var(--el-border-color-light);
  /* height: 100%; */ /* 더 이상 100% 높이를 차지하지 않음 */
  overflow-y: visible; /* 가로 메뉴는 스크롤이 필요 없음 */
}

/* [수정] admin-menu: 가로 메뉴일 때 Element Plus가 추가하는 하단 테두리 제거 */
.admin-menu.el-menu--horizontal {
  border-bottom: none !important; /* 컨테이너에 테두리가 있으므로 메뉴 자체의 테두리는 제거 */
}

/* [수정] admin-content-container: 남은 공간을 모두 차지하도록 변경 */
.admin-content-container {
  flex-grow: 1; /* 남은 세로 공간을 모두 차지 */
  padding: 20px;
  /* height: 440px; */ /* 고정 높이 제거 */
  overflow-y: auto; /* 컨텐츠가 길어지면 스크롤 생성 */
}

.main-mode-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}
.main-mode-tabs :deep(.el-tabs__item) {
  font-size: 1.1rem;
  height: 48px;
}
.search-input { margin-bottom: 1rem; }

.logo { display: flex; align-items: center; gap: 8px; }
.logo-text { font-size: 19px; font-weight: 600; color: #303133; }
.tab-content-placeholder { height: 350px; display: flex; flex-direction: column; justify-content: center; align-items: center; gap: 12px; color: var(--el-text-color-secondary); background-color: var(--el-fill-color-light); border-radius: 4px; }
.placeholder-icon { color: var(--el-text-color-placeholder); }
.concept-banner { width: 70%; height: 100%; display: flex; align-items: center; gap: 10px; background-color: #ffffff; border-radius: 4px; border: 1px solid #e4e7ed; padding: 8px; box-sizing: border-box; }
.concept-banner .el-icon { color: var(--el-color-primary); }
.concept-banner .banner-text { font-size: 14px; color: #606266; }

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