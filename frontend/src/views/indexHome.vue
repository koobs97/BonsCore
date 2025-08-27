<script setup>
import { ref, shallowRef, onMounted, defineAsyncComponent, computed, watch } from 'vue';
import {
  Search, Clock, ChatDotRound, Odometer, Moon, Sunny,
  Setting, User, CollectionTag, Tools, Operation, ChatSquare, Box, ArrowDown,
} from '@element-plus/icons-vue';
import UserInfoAvatar from "@/components/login/userInfoAvatar.vue";
import { userStore } from '@/store/userStore';
import { useRouter } from 'vue-router';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";

// router, user
const router = useRouter();
const userStoreObj = userStore();

// ------------------- 상태(State) 관리 -------------------
const isLoading = ref(true);
const isAdmin = ref(false);
// [수정] activeMainTab은 이제 'user' 또는 'UserManagement'와 같은 관리자 메뉴 URL을 직접 가리킵니다.
const activeMainTab = ref('user');
const activeUserMenuUrl = ref('');

// ------------------- 메뉴 데이터 -------------------
const userMenuItems = shallowRef([]);
// [신규] 관리자 메뉴를 드롭다운으로 사용하기 위한 데이터
const adminDropdownItems = shallowRef([]);

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

// 현재 활성화된 컴포넌트를 결정하는 로직
const currentUserComponent = computed(() => {
  if (activeMainTab.value === 'user') {
    return userComponentMap[activeUserMenuUrl.value] || null;
  }
  return null;
});

const currentAdminComponent = computed(() => {
  if (isAdmin.value && activeMainTab.value !== 'user') {
    return adminComponentMap[activeMainTab.value] || null;
  }
  return null;
});

onMounted(async () => {
  isLoading.value = true;
  try {
    const isLoggedIn = userStore().isLoggedIn;
    if (!isLoggedIn) {
      await router.push("/login");
      return;
    }

    const user = userStoreObj.getUserInfo;
    const response = await Api.post(ApiUrls.GET_MENUS, { userId: user.userId });
    const allMenus = response.data || [];
    const menuTree = buildMenuTree(allMenus);

    const adminRootMenus = menuTree.filter(menu => menu.name === '시스템 관리');
    const regularUserMenus = menuTree.filter(menu => menu.name !== '시스템 관리');

    // [수정] 관리자 메뉴를 드롭다운 아이템으로 설정
    if (user.roleId === 'ADMIN') {
      isAdmin.value = true;
      if (adminRootMenus[0]?.children) {
        adminDropdownItems.value = adminRootMenus[0].children;
      }
    }

    userMenuItems.value = regularUserMenus;
    // 첫 진입 시 활성화할 탭 설정
    if (regularUserMenus.length > 0 && regularUserMenus[0].url) {
      activeUserMenuUrl.value = regularUserMenus[0].url;
      activeMainTab.value = 'user';
    } else if (isAdmin.value && adminDropdownItems.value.length > 0) {
      activeMainTab.value = adminDropdownItems.value[0].url;
    }

    // 페이지 진입 시 저장된 테마를 읽고 적용
    const savedTheme = localStorage.getItem('theme');
    isDarkMode.value = savedTheme === 'dark';

  } catch (error) {
    console.error("메뉴 정보를 가져오는 데 실패했습니다:", error);
  } finally {
    isLoading.value = false;
  }
});

// [수정] 사용자 메뉴 선택 시 'user' 탭을 활성화하도록 수정
const handleUserMenuSelect = (url) => {
  if (url) {
    activeUserMenuUrl.value = url;
    activeMainTab.value = 'user';
  }
};

// [신규] 관리자 메뉴 선택 핸들러
const handleAdminMenuSelect = (url) => {
  if (url) {
    activeMainTab.value = url;
  }
};

const isDarkMode = ref(false);
const toggleTheme = () => {
  isDarkMode.value = !isDarkMode.value;
};
// isDarkMode의 변경을 감지하여 <html> 클래스와 localStorage를 업데이트
watch(isDarkMode, (newVal) => {
  if (newVal) {
    document.documentElement.classList.add('dark');
    localStorage.setItem('theme', 'dark'); // 변경된 테마를 localStorage에 저장
  } else {
    document.documentElement.classList.remove('dark');
    localStorage.setItem('theme', 'light'); // 변경된 테마를 localStorage에 저장
  }
});
const goToGitHub = () => {
  window.open('https://github.com/koobs97/BonsCore/tree/main', '_blank');
}
</script>

<template>
  <div class="page-container">
    <!-- 상단 네비게이션 바 (기존과 동일) -->
    <el-header class="main-header">
      <a href="/" class="logo-container">
        <!-- 아이콘 영역 -->
        <div class="logo-icon-wrapper">
          <div class="logo-icon"></div>
        </div>
        <!-- 텍스트 영역 -->
        <div>
          <span class="logo-main-text">BONS</span>
          <span class="logo-sub-text">Project</span>
        </div>
      </a>
      <div class="header-actions">
        <el-button class="custom-image-button" :icon="ChatSquare" />
        <el-tooltip content="테마 변경" placement="bottom">
          <el-button class="custom-image-button" :icon="isDarkMode ? Moon : Sunny" @click="toggleTheme"/>
        </el-tooltip>
        <el-tooltip content="깃허브 바로가기" placement="bottom">
          <el-button class="custom-image-button" @click="goToGitHub">
            <img class="theme-sensitive" src="@/assets/images/github_icon.png" alt="custom icon"/>
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
              <div style="display: flex; flex-direction: column; gap: 4px; text-align: left;">
                <h3 style="margin: 0; font-family: 'Poppins', sans-serif; font-size: 1.1rem; font-weight: 600; color: var(--main-header-text-color2);">개인 프로젝트 공간</h3>
                <p style="margin: 0; font-family: 'Noto Sans KR', sans-serif; font-size: 0.85rem; color: var(--main-header-text-color1);">Powered by Spring Boot, Vue 3</p>
              </div>
            </div>


          <UserInfoAvatar/>
        </div>

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
              <el-tab-pane v-if="isAdmin" :name="activeMainTab === 'user' ? 'admin' : activeMainTab">
                <template #label>
                  <el-dropdown trigger="hover" @command="handleAdminMenuSelect">
                <span class="admin-tab-label"> <!-- 기존 스타일 재사용 -->
                  관리자
                  <el-icon class="el-icon--right"><arrow-down/></el-icon>
                </span>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item
                            v-for="menu in adminDropdownItems"
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
                <!-- 관리자 탭 콘텐츠 -->
                <div class="content-card">
                  <div class="admin-content-container">
                    <component :is="currentAdminComponent" v-if="currentAdminComponent"/>
                    <div v-else class="tab-content-placeholder">
                      <p>관리자 메뉴를 선택해주세요.</p>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url("https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css");

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
  background-color: var(--el-bg-color);
  border-bottom: 1px solid #e4e7ed;
  padding: 0;
  box-sizing: border-box;
  flex-shrink: 0;
}
.logo-container {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  text-decoration: none; /* a 태그의 밑줄 제거 */
}

/* 2. 아이콘을 감싸는 배경 */
.logo-icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 8px; /* 부드러운 사각형 */
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--el-color-primary-light-9); /* 아주 연한 배경색 */
  transition: transform 0.2s ease;
}

/* 3. 실제 아이콘 (핵심 색상) */
.logo-icon {
  width: 18px;
  height: 18px;
  border-radius: 6px;
  background-color: var(--el-color-primary);
}

/* 4. 텍스트를 세로로 정렬하기 위한 래퍼 */
.logo-text-wrapper {
  display: flex;
  flex-direction: column;
}

/* 5. 메인 텍스트 (BONS) 스타일 */
.logo-main-text {
  font-family: 'Poppins', sans-serif;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.2;
  transition: color 0.2s ease;
  letter-spacing: 0.5px; /* 글자 간격을 살짝 주어 세련미 추가 */
}

/* 6. 서브 텍스트 (Project) 스타일 */
.logo-sub-text {
  font-family: 'Poppins', sans-serif;
  font-size: 0.75rem;
  text-align: left;
  margin-left: 4px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
  line-height: 1;
}

/* 7. 마우스를 올렸을 때의 인터랙션 효과 */
.logo-container:hover .logo-icon-wrapper {
  transform: rotate(10deg) scale(1.05); /* 아이콘이 살짝 회전하며 커짐 */
}

.logo-container:hover .logo-main-text {
  color: var(--el-color-primary); /* 메인 텍스트 색상이 테마 색상으로 변경 */
}
.custom-image-button {
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 50%; /* 완전한 원형으로 변경 */
  border: none; /* 테두리 제거 */
  background-color: transparent; /* 배경색 제거 */
  color: var(--el-text-color-secondary);
  font-size: 18px;
  transition: all 0.2s ease;
  outline: none;
}

.custom-image-button:hover {
  background-color: var(--el-fill-color-light); /* 호버 시 은은한 배경색 */
  color: var(--el-text-color-primary);
}

.custom-image-button img {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: block;
  margin: auto; /* 이미지를 버튼 중앙에 위치 */
  opacity: 0.8;
}
.custom-image-button:hover img {
  opacity: 1;
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
.content-card {
  width: 100%;
  height: 680px;
  min-height: 600px;
  border-radius: 4px;
  box-sizing: border-box;
  box-shadow: 0 4px 12px rgba(108, 92, 231, 0.05);
}
.user-content-container {
  height: 670px; /* 관리자 패널과 높이를 맞추거나 조절 */
  overflow-y: auto;
  padding: 0;
  margin: 0; /* 부모 패딩 고려 */
}
.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
  color: var(--el-overlay-color-light); /* Element Plus 테마 색상 사용 */
  font-size: 1rem; /* [수정] 탭과 동일하게 1rem으로 명시적으로 지정 */
  font-weight: 500; /* [추가] 탭 기본 폰트 두께와 맞춤 */
  outline: none; /* 클릭 시 테두리 제거 */
  padding: 0 0 0 20px;
}
.el-dropdown-link:hover {
  color: var(--el-color-primary-light-3);
}
.admin-tab-label {
  cursor: pointer; /* 클릭 가능한 요소임을 나타내기 위해 커서 변경 */
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--el-overlay-color-light); /* '서비스' 탭과 동일한 색상 적용 */
  font-size: 1rem;                 /* '서비스' 탭과 동일한 폰트 크기 적용 */
  font-weight: 500;                /* '서비스' 탭과 동일한 폰트 두께 적용 */
  outline: none;                   /* 클릭 시 나타나는 포커스 선 제거 */
}
.admin-tab-label:hover {
  color: var(--el-color-primary-light-3);
}
.admin-content-container {
  flex-grow: 1;
  overflow-y: auto;
}
.tab-content-placeholder {
  height: 620px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 12px;
  color: var(--el-text-color-secondary);
  background-color: var(--el-fill-color-light);
  border-radius: 4px;
  margin: 4px 0 0 0;
}
.placeholder-icon {
  color: var(--el-text-color-placeholder);
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
.main-mode-tabs {
  font-family: 'Pretendardaw', sans-serif;
  width: 100%;
  /* 탭 헤더와 콘텐츠 카드 사이의 연결을 위해 margin-bottom을 음수값으로 설정 */
  margin-bottom: -1px;
  z-index: 1; /* 콘텐츠 카드보다 위에 오도록 설정 */
}
/* 탭 헤더(el-tabs__header) 스타일링 */
.main-mode-tabs :deep(.el-tabs__header) {
  margin: 0;
  border: 1px solid var(--el-border-color-light);
  background-color: var(--el-fill-color-light);
  /* 위쪽만 둥글게 설정 */
  border-radius: 4px 4px 0 0;
}

/* 탭 네비게이션 영역(el-tabs__nav-wrap)의 기본 하단 라인 제거 */
.main-mode-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

/* 개별 탭 아이템(el-tabs__item) 스타일 */
.main-mode-tabs :deep(.el-tabs__item) {
  font-size: 1rem;
  height: 44px;
  padding: 0 20px;
  border-top: 3px solid transparent; /* 비활성 탭 상단 테두리 투명 처리 */
}

/* 활성화된 탭 아이템 스타일 */
.main-mode-tabs :deep(.el-tabs__item.is-active) {
  background-color: var(--el-bg-color); /* 흰색 배경 */
  border-bottom-color: transparent; /* 하단 테두리 제거하여 콘텐츠와 연결 */
  /* 위쪽 모서리만 둥글게 하여 카드와 자연스럽게 연결 */
  border-radius: 3px 3px 0 0;
  margin-bottom: 6px;
}

.main-mode-tabs :deep(.el-tabs__item:last-child) {
  padding-right: 20px !important;
}

/* 드롭다운이 포함된 탭의 padding을 0으로 설정 */
.main-mode-tabs :deep(.el-tabs__item:has(.el-dropdown)) {
  padding: 0 20px;
}

/* 2. 가상 요소를 사용해 우리가 원하는 너비의 '새로운 막대'를 생성합니다. */
.main-mode-tabs :deep(.el-tabs__active-bar)::before {
  content: "";
  position: absolute;
  /* left와 right를 -20px로 설정하여 양옆으로 20px씩 늘려줍니다. */
  left: -20px;
  right: -20px;
  bottom: 0;
  height: 2px; /* 원래 막대와 동일한 높이 */
  background-color: var(--el-color-primary); /* 원래 막대와 동일한 색상 */
}
/* 드롭다운 메뉴 전체적인 디자인 */
:deep(.el-dropdown__menu) {
  /* 그림자로 입체감 부여 */
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  border-radius: 8px; /* 부드러운 곡선 */
  border: 1px solid #e4e7ed;
  padding: 6px; /* 내부 여백 */
  background-color: #ffffff;
}

/* 드롭다운 메뉴의 각 아이템 */
:deep(.el-dropdown-menu__item) {
  font-family: 'Noto Sans KR', sans-serif; /* 가독성 좋은 폰트 적용 */
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  border-radius: 6px; /* 아이템에도 부드러운 곡선 적용 */
  padding: 0 12px;
  height: 38px;
  line-height: 38px;
  transition: all 0.2s ease; /* 부드러운 전환 효과 */
}

/* 마우스를 올렸을 때(hover) 아이템 스타일 */
:deep(.el-dropdown-menu__item:hover) {
  background-color: #f5f7fa; /* 은은한 배경색 변경 */
  color: var(--el-color-primary); /* 테마 색상으로 텍스트 색상 변경 */
}

/* 아이템 내부의 아이콘 스타일 */
:deep(.el-dropdown-menu__item .el-icon) {
  margin-right: 10px; /* 아이콘과 텍스트 사이 간격 */
  font-size: 16px;
}

/* 드롭다운 메뉴가 나타날 때 화살표(popper) 숨김 */
:deep(.el-popper.is-light) {
  border: none; /* 기본 테두리 제거 */
}

:deep(.el-popper.is-light .el-popper__arrow::before) {
  display: none; /* 화살표 숨기기 */
}
/* 기본 (라이트모드) */
.theme-sensitive {
  filter: none;
  transition: filter 0.3s ease; /* 모드 전환시 부드럽게 */
}

/* 다크모드 */
.dark .theme-sensitive {
  filter: invert(1) hue-rotate(180deg);
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