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

// 1. Pinia 인스턴스를 먼저 생성하고 앱에 등록합니다.
//    이렇게 해야 아래에서 userStore()를 즉시 사용할 수 있습니다.
const pinia = createPinia();
app.use(pinia);

// 2. 라우터를 사용하기 "전"에 로그인 정보를 먼저 복원합니다.
const savedUserInfo = localStorage.getItem('userInfo');
if (savedUserInfo) {
    try {
        const userInfo = JSON.parse(savedUserInfo);
        // userStore()가 정상적으로 동작하려면 app.use(pinia)가 먼저 실행되어야 합니다.
        userStore().setUserInfo(userInfo);
    } catch (e) {
        console.error("Failed to parse user info from localStorage", e);
        localStorage.removeItem('userInfo'); // 잘못된 데이터는 삭제
    }
}

// 3. 이제 스토어에 정보가 복원된 상태에서 라우터를 등록합니다.
//    이렇게 하면 네비게이션 가드가 실행될 때 정확한 로그인 상태를 참조할 수 있습니다.
app.use(router);

// ★★★★★★★★★★★★★★★★★★★★★★★★★★★
// ★          여기까지 수정           ★
// ★★★★★★★★★★★★★★★★★★★★★★★★★★★

// 화면 모드 설정 유지 (이 로직은 순서에 크게 상관없지만, 렌더링 직전에 두는 것이 좋습니다.)
const savedTheme = localStorage.getItem('theme');
if (savedTheme === 'dark') {
    document.documentElement.classList.add('dark');
} else {
    document.documentElement.classList.remove('dark');
}


app.config.globalProperties.$ELEMENT = { zIndex: 5000 };
// ElementPlus 아이콘을 전역으로 등록
Object.entries(ElIcons).forEach(([key, component]) => {
    app.component(key, component);
});

// 나머지 플러그인들을 등록하고 마운트합니다.
app
    // .use(router) // 위로 이동했으므로 여기선 삭제
    // .use(createPinia()) // 위에서 app.use(pinia)로 대체되었으므로 삭제
    .use(ElementPlus, {
        locale: ko
    })
    .use(VCalendar, {})
    .directive('byte-limit', byteLimit)
    .component('AgGridVue', AgGridVue)
    .mount('#app')

const footerApp = createApp(TheFooter);
footerApp.mount('#footer-container');

// 아래 로직들은 위로 이동했습니다.
// // 새로고침 시 로그인 정보 복원
// // 화면 모드 설정 유지