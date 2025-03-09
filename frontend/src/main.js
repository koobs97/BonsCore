import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from '../router';

// App
const app = createApp(App);


// element-plus
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import 'element-plus/dist/index.css';
import ElementPlus from 'element-plus';


import { createPinia } from "pinia";

app
    .use(router)
    .use(createPinia())
    .use(ElementPlus)
    .mount('#app')
