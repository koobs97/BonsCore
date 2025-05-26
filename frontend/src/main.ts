import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from '../router';
import * as ElIcons from '@element-plus/icons-vue';
import { userStore } from '@/store/userStore';

// App
const app = createApp(App);


// element-plus
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import 'element-plus/dist/index.css';
import ElementPlus from 'element-plus';
app.config.globalProperties.$ELEMENT = { zIndex: 5000 };
// ElementPlus 아이콘을 전역으로 등록
Object.entries(ElIcons).forEach(([key, component]) => {
    app.component(key, component);
});

import { createPinia } from "pinia";

app
    .use(router)
    .use(createPinia())
    .use(ElementPlus)
    .mount('#app')

// ✅ 로그인 정보 복원
const savedUserInfo = localStorage.getItem('userInfo');
if (savedUserInfo) {
    const userInfo = JSON.parse(savedUserInfo);
    userStore().setUserInfo(userInfo);
}