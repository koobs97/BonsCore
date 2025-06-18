import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from '../router';
import * as ElIcons from '@element-plus/icons-vue';
import { userStore } from '@/store/userStore';
import { createPinia } from "pinia";

// v-calendar
import VCalendar from 'v-calendar';
import 'v-calendar/style.css';

// element-plus
import 'element-plus/dist/index.css';
import ElementPlus from 'element-plus';
import ko from 'element-plus/es/locale/lang/ko';

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
    .mount('#app')

// ✅ 로그인 정보 복원
const savedUserInfo = localStorage.getItem('userInfo');
if (savedUserInfo) {
    const userInfo = JSON.parse(savedUserInfo);
    userStore().setUserInfo(userInfo);
}