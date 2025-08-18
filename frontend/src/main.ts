import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from '../router';
import * as ElIcons from '@element-plus/icons-vue';
import { userStore } from '@/store/userStore';
import { createPinia } from "pinia";

import TheFooter from "@/components/layout/TheFooter.vue";

// custom-directives
import { byteLimit } from '@/directives/byteLimit'

// v-calendar
import VCalendar from 'v-calendar';
import 'v-calendar/style.css';

// element-plus
import 'element-plus/dist/index.css';
import ElementPlus from 'element-plus';
import ko from 'element-plus/es/locale/lang/ko';
import 'element-plus/theme-chalk/dark/css-vars.css' // Element Plus 다크 모드 CSS

// ag-grid
import { AgGridVue } from 'ag-grid-vue3'
import 'ag-grid-community/styles/ag-grid.css'
import 'ag-grid-community/styles/ag-theme-alpine.css'
import { ModuleRegistry } from 'ag-grid-community'
import { AllCommunityModule } from 'ag-grid-community'
ModuleRegistry.registerModules([AllCommunityModule])

// App
const app = createApp(App);

app.config.globalProperties.$ELEMENT = { zIndex: 5000 };
// ElementPlus 아이콘을 전역으로 등록
Object.entries(ElIcons).forEach(([key, component]) => {
    app.component(key, component);
});

app
    .use(router)
    .use(createPinia())
    .use(ElementPlus, {
        locale: ko
    })
    .use(VCalendar, {})
    .directive('byte-limit', byteLimit)
    .component('AgGridVue', AgGridVue)
    .mount('#app')

const footerApp = createApp(TheFooter);
// 만약 푸터가 Vuex 스토어나 다른 플러그인을 사용한다면 여기에도 등록해줘야 합니다.
// footerApp.use(store);
footerApp.mount('#footer-container');

// 새로고침 시 로그인 정보 복원
const savedUserInfo = localStorage.getItem('userInfo');
if (savedUserInfo) {
    const userInfo = JSON.parse(savedUserInfo);
    userStore().setUserInfo(userInfo);
}

// 화면 모드 설정 유지
const savedTheme = localStorage.getItem('theme');
if (savedTheme === 'dark') {
    // 앱이 로드되기 전에 html 태그에 'dark' 클래스를 즉시 추가
    document.documentElement.classList.add('dark');
} else {
    // 라이트 모드이거나 설정이 없을 경우 'dark' 클래스를 제거
    document.documentElement.classList.remove('dark');
}