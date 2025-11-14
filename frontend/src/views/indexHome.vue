<script setup lang="ts">
import { ref, shallowRef, onMounted, defineAsyncComponent, computed, watch } from 'vue';
import {
  Search, Clock, ChatDotRound, Odometer, Moon, Sunny,
  Setting, User, CollectionTag, Tools, Operation, Link, ArrowDown, CopyDocument, Share, Close, InfoFilled
} from '@element-plus/icons-vue';
import { ElMessage } from "element-plus";
import { userStore } from '@/store/userStore';
import { useRouter } from 'vue-router';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import UserInfoAvatar from "@/components/login/userInfoAvatar.vue";
import { useI18n } from "vue-i18n";

import TranslateIcon from '@/assets/images/translation_icon.png';

const activeName = ref('default')
const githubUrl = ref('https://github.com/koobs97/BonsCore/tree/main');
const notionUrl = ref('https://bonsang-note.notion.site/cd34738bd0b34dccb15c5b5cb74904d1?source=copy_link');

// i18n
const { t, locale } = useI18n();

// 언어 변경 팝오버 관련 로직
const languagePopoverRef = ref();

// router, user
const router = useRouter();
const userStoreObj = userStore();

// ------------------- 상태(State) 관리 -------------------
const isLoading = ref(true);
const isAdmin = ref(false);
const activeMainTab = ref('user');
const activeUserMenuUrl = ref('');

// ------------------- 메뉴 데이터 -------------------
const userMenuItems = shallowRef([]);
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

/**
 * 언어 변경 핸들러 함수
 * @param newLang 'ko' 또는 'en'
 */
const onLanguageChange = (newLang: 'ko' | 'en') => {
  locale.value = newLang; // i18n의 locale 상태를 변경
  localStorage.setItem('language', newLang); // 사용자의 선택을 브라우저에 저장
};

// 메뉴 트리 변환 함수 (기존과 동일)
const buildMenuTree = (flatMenus, parentId) => {
  const isRootCall = parentId === undefined;

  return flatMenus
      .filter(menu => {
        if (isRootCall) {
          return menu.parentMenuId === null || menu.parentMenuId === '';
        }
        return menu.parentMenuId === parentId;
      })
      .sort((a, b) => Number(a.sortOrder) - Number(b.sortOrder))
      .map(menu => {
        const children = buildMenuTree(flatMenus, menu.menuId);
        return {
          id: menu.menuId,
          // DB에서 받은 menu.menuName을 t() 함수로 감싸 번역된 이름을 사용
          name: t(`main.menus.${menu.menuName}`),
          label: t(`main.menus.${menu.menuName}`),
          url: menu.menuUrl || '',
          // iconMap의 키는 DB 원본 값이므로 menu.menuName을 그대로 사용
          icon: iconMap[menu.menuName] || iconMap['기본아이콘'],
          description: `메뉴: ${t(`menus.${menu.menuName}`)}`,
          isVisible: menu.isVisible === 'Y',
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

    // isVisible이 'Y'인 메뉴만 먼저 필터링
    const visibleMenus = (response.data || []).filter(menu => menu.isVisible === 'Y');

    // isVisible이 'N'인 루트 메뉴(컨테이너 역할)들을 찾음
    const allMenus = response.data || [];
    const menuTree = buildMenuTree(allMenus);

    // 2. 생성된 트리에서 '서비스'와 '시스템 관리' 루트를 찾습니다.
    const serviceRoot = menuTree.find(menu => menu.name === t('main.menus.서비스'));
    const adminRoot = menuTree.find(menu => menu.name === t('main.menus.시스템 관리'));

    // 3. 각 루트의 '자식' 메뉴들 중 isVisible이 true인 것만 화면에 표시할 메뉴로 할당합니다.
    if (serviceRoot && serviceRoot.children) {
      userMenuItems.value = serviceRoot.children.filter(child => child.isVisible);
    }

    if (user.roleId === 'ADMIN') {
      isAdmin.value = true;
      if (adminRoot && adminRoot.children) {
        adminDropdownItems.value = adminRoot.children.filter(child => child.isVisible);
      }
    }

    console.log(userMenuItems.value.length)
    console.log(userMenuItems.value[0].url)

    // 첫 진입 시 활성화할 탭 설정 (이전 답변의 로직과 거의 동일)
    if (userMenuItems.value.length > 0 && userMenuItems.value[0].url) {
      activeMainTab.value = 'user';
      activeUserMenuUrl.value = userMenuItems.value[0].url;
    } else if (isAdmin.value && adminDropdownItems.value.length > 0) {
      activeMainTab.value = adminDropdownItems.value[0].url; // 관리자 탭은 URL을 바로 name으로 사용
    } else {
      activeMainTab.value = 'no-service';
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
const goToNotion = () => {
  window.open('https://bonsang-note.notion.site/cd34738bd0b34dccb15c5b5cb74904d1?source=copy_link', '_blank');
}
const copyToClipboard1 = async () => {
  try {
    await navigator.clipboard.writeText(githubUrl.value);
    ElMessage({
      message: t('main.messages.urlCopied'),
      type: 'success',
      duration: 2000,
    });
  } catch (err) {
    console.error('클립보드 복사 실패:', err);
    ElMessage({
      message: t('main.messages.copyFailed'),
      type: 'error',
    });
  }
};
const copyToClipboard2 = async () => {
  try {
    await navigator.clipboard.writeText(notionUrl.value);
    ElMessage({
      message: t('main.messages.urlCopied'),
      type: 'success',
      duration: 2000,
    });
  } catch (err) {
    console.error('클립보드 복사 실패:', err);
    ElMessage({
      message: t('main.messages.copyFailed'),
      type: 'error',
    });
  }
};
</script>

<template>
  <div class="page-container">
    <!-- 상단 네비게이션 바 -->
    <el-header class="main-header">

      <a href="/" class="logo-container">
        <div class="logo-icon-wrapper">
          <div class="logo-icon"></div>
        </div>
        <div>
          <span class="logo-main-text">BONS</span>
          <span class="logo-sub-text">Project</span>
        </div>
      </a>

      <div class="header-actions">
        <el-popover
            ref="languagePopoverRef"
            placement="bottom-end"
            :width="270"
            trigger="click"
            popper-class="language-popover"
        >
          <!-- 팝오버를 여는 버튼 -->
          <template #reference>
            <el-button class="custom-image-button">
              <img class="theme-icon" :src="TranslateIcon" alt="언어"/>
            </el-button>
          </template>
          <!-- 팝오버 내용 -->
          <div class="language-popover-content">
            <!-- 헤더 -->
            <div class="popover-header">
              <span class="popover-title-text">{{ t('main.languagePopover.selectLanguage') }}</span>
              <el-button
                  :icon="Close"
                  link
                  class="close-btn"
                  @click="languagePopoverRef?.hide()"
              />
            </div>
            <el-divider />
            <!-- 본문 (언어 선택 버튼) -->
            <div class="popover-main">
              <div class="language-buttons">
                <el-button
                    :class="{ 'is-active': locale === 'ko' }"
                    @click="onLanguageChange('ko')"
                >
                  한국어
                </el-button>
                <el-button
                    :class="{ 'is-active': locale === 'en' }"
                    @click="onLanguageChange('en')"
                >
                  English
                </el-button>
              </div>
              <div class="language-warning-box">
                <el-icon :size="15" color="var(--el-color-warning)">
                  <InfoFilled />
                </el-icon>
                <el-text class="warning-text" type="warning">
                  {{ t('main.languagePopover.warningText') }}
                </el-text>
              </div>
            </div>
            <el-divider />
            <!-- 푸터 -->
            <div class="popover-footer">
              <p class="footer-text">Bons Translate</p>
            </div>
          </div>
        </el-popover>

        <el-tooltip :content="t('main.header.changeTheme')" placement="bottom">
          <el-button class="custom-image-button" :icon="isDarkMode ? Moon : Sunny" @click="toggleTheme"/>
        </el-tooltip>

        <el-popover :width="410" trigger="click">
          <template #reference>
            <el-button class="custom-image-button">
              <el-icon><Share /></el-icon>
            </el-button>
          </template>
          <div class="my-custom-tabs">
            <el-tabs v-model="activeName">
              <el-tab-pane name="default">
                <template #label>
                <span style="font-size: 14px; font-weight: 600;">
                      GitHub
                    </span>
                </template>
                <img class="theme-sensitive-poster" src="@/assets/images/github-poster.png" alt="custom icon"/>
                <div style="display: flex; align-items: center; margin-top: 8px;">
                  <img class="theme-sensitive" src="@/assets/images/github_icon.png" alt="custom icon"/>
                  <el-input readonly v-model="githubUrl">
                  </el-input>
                  <el-button link @click="copyToClipboard1" style="outline: none;">
                    <el-icon style="font-size: 18px; margin-left: 8px;"><CopyDocument /></el-icon>
                  </el-button>
                  <el-button link style="font-size: 18px; margin-left: 8px; outline: none;" :icon="Link" @click="goToGitHub" />
                </div>
                <el-divider style="margin: 12px 0 16px 0;" />
                <div style="display: flex;align-items: center;">
                  <el-icon style="font-size: 14px;"><CopyDocument /></el-icon>
                  <el-text style="font-size: 12px; margin-left: 4px;">{{ t('main.sharePopover.copyUrl') }}</el-text>
                  <el-icon style="font-size: 14px; margin-left: 12px;"><Link /></el-icon>
                  <el-text style="font-size: 12px; margin-left: 4px;">{{ t('main.sharePopover.visitSite') }}</el-text>
                </div>
              </el-tab-pane>
              <el-tab-pane name="newTab">
                <template #label>
                 <span style="font-size: 14px; font-weight: 600;">
                  Notion
                </span>
                </template>
                <img class="theme-sensitive-poster" src="@/assets/images/notion-poster.png" alt="custom icon"/>
                <div style="display: flex; align-items: center; margin-top: 8px;">
                  <img class="theme-sensitive" src="@/assets/images/notion_icon.png" alt="custom icon"/>
                  <el-input readonly v-model="notionUrl">
                  </el-input>
                  <el-button link @click="copyToClipboard2" style="outline: none;">
                    <el-icon style="font-size: 18px; margin-left: 8px;"><CopyDocument /></el-icon>
                  </el-button>
                  <el-button link style="font-size: 18px; margin-left: 8px; outline: none;" :icon="Link" @click="goToNotion" />
                </div>
                <el-divider style="margin: 12px 0 16px 0;" />
                <div style="display: flex;align-items: center;">
                  <el-icon style="font-size: 14px;"><CopyDocument /></el-icon>
                  <el-text style="font-size: 12px; margin-left: 4px;">{{ t('main.sharePopover.copyUrl') }}</el-text>
                  <el-icon style="font-size: 14px; margin-left: 12px;"><Link /></el-icon>
                  <el-text style="font-size: 12px; margin-left: 4px;">{{ t('main.sharePopover.visitSite') }}</el-text>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>

        </el-popover>
      </div>
    </el-header>

    <div class="content-layout-wrapper">
      <div class="main-content-area">
        <!-- 정보 래퍼 -->
        <div class="info-wrapper">
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
                <h3 style="margin: 0; font-family: 'Poppins', sans-serif; font-size: 1.1rem; font-weight: 600; color: var(--main-header-text-color2);">{{ t('main.banner.title') }}</h3>
                <p style="margin: 0; font-family: 'Noto Sans KR', sans-serif; font-size: 0.85rem; color: var(--main-header-text-color1);">Powered by Spring Boot, Vue 3</p>
              </div>
            </div>
          <UserInfoAvatar/>
        </div>

        <el-skeleton :rows="10" animated v-if="isLoading"/>
        <el-tabs v-model="activeMainTab" class="main-mode-tabs" v-else>

          <el-tab-pane name="user">
            <template #label>
              <el-dropdown trigger="hover" @command="handleUserMenuSelect">
                <span class="el-dropdown-link">
                  {{ t('main.tabs.service') }}
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

            <div v-if="userMenuItems.length > 0">
              <!-- 3. 선택된 메뉴에 따라 동적으로 컴포넌트를 렌더링 -->
              <div class="user-content-container">
                <keep-alive>
                  <component :is="currentUserComponent" :key="activeUserMenuUrl"/>
                </keep-alive>

                <!-- 4. 컴포넌트를 찾지 못했을 경우의 폴백 UI -->
                <div v-if="!currentUserComponent && activeUserMenuUrl" class="tab-content-placeholder">
                  <el-icon :size="32" class="placeholder-icon">
                    <Tools/>
                  </el-icon>
                  <p>{{ t('main.placeholders.componentNotFound', { componentName: activeUserMenuUrl }) }}</p>
                  <p style="font-size: 12px; color: #909399;">{{ t('main.placeholders.checkComponentPath', { componentName: activeUserMenuUrl }) }}</p>
                </div>
              </div>
            </div>

            <!-- 사용 가능한 서비스 메뉴가 없을 경우 -->
            <div v-else class="tab-content-placeholder">
              <p>{{ t('main.placeholders.noServiceMenu') }}</p>
            </div>
          </el-tab-pane>

            <!-- 관리자 탭 -->
          <el-tab-pane v-if="isAdmin" :name="activeMainTab === 'user' ? 'admin' : activeMainTab">
            <template #label>
              <el-dropdown trigger="hover" @command="handleAdminMenuSelect">
              <span class="admin-tab-label">
                {{ t('main.tabs.admin') }}
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
                  <p>{{ t('main.placeholders.selectAdminMenu') }}</p>
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
.page-container {
  height: 120vh;
  max-height: 860px;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 32px auto;
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
  text-decoration: none;
}
.logo-icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--el-color-primary-light-9);
  border: 1px solid var(--el-border-color);
  transition: transform 0.2s ease;
}
.logo-icon {
  width: 18px;
  height: 18px;
  border-radius: 6px;
  background-color: var(--el-color-primary);
}
.logo-text-wrapper {
  display: flex;
  flex-direction: column;
}
.logo-main-text {
  font-family: 'Poppins', sans-serif;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.2;
  transition: color 0.2s ease;
  letter-spacing: 0.5px;
}
.logo-sub-text {
  font-family: 'Poppins', sans-serif;
  font-size: 0.75rem;
  text-align: left;
  margin-left: 4px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
  line-height: 1;
}
.logo-container:hover .logo-icon-wrapper {
  transform: rotate(10deg) scale(1.05);
}
.logo-container:hover .logo-main-text {
  color: var(--el-color-primary);
}
.custom-image-button {
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 50%;
  border: none;
  background-color: transparent;
  color: var(--el-text-color-secondary);
  font-size: 18px;
  transition: all 0.2s ease;
  outline: none;
}
.custom-image-button:hover {
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
}
.custom-image-button img {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: block;
  margin: auto;
}
.custom-image-button:hover img {
  opacity: 1;
}
.custom-image-button .theme-icon {
  opacity: 0.6;
  filter: none;
  transition: opacity 0.3s, filter 0.3s;
}
.dark .custom-image-button .theme-icon {
  filter: brightness(0) invert(1);
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
  height: 670px;
  min-height: 600px;
  border-radius: 4px;
  box-sizing: border-box;
  box-shadow: 0 4px 12px rgba(108, 92, 231, 0.05);
}
.user-content-container {
  height: 670px;
  overflow-y: auto;
  padding: 0;
  margin: 0;
}
.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
  color: var(--el-overlay-color-light);
  font-size: 1rem;
  font-weight: 500;
  outline: none;
  padding: 0 0 0 20px;
}
.el-dropdown-link:hover {
  color: var(--el-color-primary-light-3);
}
.admin-tab-label {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--el-overlay-color-light);
  font-size: 1rem;
  font-weight: 500;
  outline: none;
}
.admin-tab-label:hover {
  color: var(--el-color-primary-light-3);
}
.admin-content-container {
  flex-grow: 1;
  overflow-y: auto;
  height: 670px;
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
  width: 24px;
  height: 24px;
}
.main-mode-tabs {
  font-family: 'Pretendardaw', sans-serif;
  width: 100%;
  margin-bottom: -1px;
  z-index: 1;
}
.main-mode-tabs :deep(.el-tabs__header) {
  margin: 0;
  border: 1px solid var(--el-border-color-light);
  background-color: var(--el-fill-color-light);
  border-radius: 4px 4px 0 0;
}
.main-mode-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}
.main-mode-tabs :deep(.el-tabs__item) {
  font-size: 1rem;
  height: 44px;
  padding: 0 20px;
  border-top: 3px solid transparent;
}
.main-mode-tabs :deep(.el-tabs__item.is-active) {
  background-color: var(--el-bg-color);
  border-bottom-color: transparent;
  border-radius: 3px 3px 0 0;
  margin-bottom: 6px;
}
.main-mode-tabs :deep(.el-tabs__item:last-child) {
  padding-right: 20px !important;
}
.main-mode-tabs :deep(.el-tabs__item:has(.el-dropdown)) {
  padding: 0 20px;
}
.main-mode-tabs :deep(.el-tabs__active-bar)::before {
  content: "";
  position: absolute;
  left: -20px;
  right: -20px;
  bottom: 0;
  height: 2px;
  background-color: var(--el-color-primary);
}
:deep(.el-popper.is-light) {
  margin-top: 8px !important;
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
}
:deep(.el-popper.is-light .el-popper__arrow::before) {
  display: none;
}
:deep(.el-dropdown__menu) {
  max-height: 300px;
  overflow-y: auto;
  overflow-x: hidden;

  box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.07), 0 4px 6px -4px rgb(0 0 0 / 0.07);
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  padding: 8px;
  background-color: var(--el-bg-color-overlay);

  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
    margin: 8px 0;
  }
  &::-webkit-scrollbar-thumb {
    background-color: var(--el-border-color-lighter);
    border-radius: 10px;
  }
  &::-webkit-scrollbar-thumb:hover {
    background-color: var(--el-border-color);
  }
  scrollbar-width: thin;
  scrollbar-color: var(--el-border-color-lighter) transparent;
}
:deep(.el-dropdown-menu__item) {
  position: relative;
  display: flex;
  align-items: center;
  font-family: 'Pretendard', 'Noto Sans KR', sans-serif;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-regular);
  border-radius: 8px;
  padding: 0 12px 0 20px;
  height: 42px;
  line-height: 42px;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}
:deep(.el-dropdown-menu__item::before) {
  content: "";
  position: absolute;
  left: 8px;
  top: 50%;
  height: 50%;
  width: 3px;
  background-color: var(--el-color-primary);
  border-radius: 2px;
  transform: translateY(-50%) scaleY(0);
  opacity: 0;
  transition: transform 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94), opacity 0.2s ease;
}
:deep(.el-dropdown-menu__item:not(.is-disabled):hover),
:deep(.el-dropdown-menu__item:not(.is-disabled):focus-visible) {
  outline: none;
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
:deep(.el-dropdown-menu__item:not(.is-disabled):hover::before),
:deep(.el-dropdown-menu__item:not(.is-disabled):focus-visible::before) {
  transform: translateY(-50%) scaleY(1);
  opacity: 1;
}
:deep(.el-dropdown-menu__item .el-icon) {
  margin-right: 12px;
  font-size: 18px;
  transition: color 0.2s ease;
}
:deep(.el-dropdown-menu__item:not(.is-disabled):hover .el-icon) {
  color: var(--el-color-primary);
}
:deep(.el-dropdown-menu__item .el-icon) {
  margin-right: 12px;
  font-size: 18px;
  transition: color 0.2s ease;
}
:deep(.el-dropdown-menu__item:not(.is-disabled):hover .el-icon) {
  color: var(--el-color-primary);
}
.my-custom-tabs :deep(.el-tabs__item) {
  padding: 0 12px;
}
.theme-sensitive-poster {
  filter: none;
  transition: filter 0.3s ease;
  width: 380px;
  height: auto;
  margin-right: 4px;
  border: 2px solid;
}
.theme-sensitive {
  filter: none;
  transition: filter 0.3s ease;
  width: auto;
  height: 24px;
  margin-right: 4px;
  border: 2px solid;
  padding: 2px;
}
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
.language-warning-box {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 8px 10px;
  border-radius: 6px;
  background-color: var(--el-color-warning-light-9);
}
.warning-text {
  font-size: 11px;
  line-height: 1.4;
}
</style>
<style>
.language-popover {
  padding: 0 !important;
  border-radius: 12px !important;
  border: 1px solid var(--el-border-color-light) !important;
  box-shadow: var(--el-box-shadow-light) !important;
}
.language-popover-content {
  background-color: var(--el-bg-color-overlay);
  border-radius: 12px;
}
.popover-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 8px 12px 16px;
}
.popover-title-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.close-btn {
  font-size: 16px;
}
.language-popover-content .el-divider {
  margin: 0;
}
.popover-main {
  padding: 12px;
}
.language-buttons {
  display: flex;
  justify-content: center;
  gap: 2px;
}
.language-buttons .el-button {
  flex: 1;
  margin-left: 0;
  transition: all 0.2s ease;
}
.language-buttons .el-button.is-active {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
}
.popover-footer {
  padding: 10px 16px;
  text-align: center;
}
.footer-text {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>