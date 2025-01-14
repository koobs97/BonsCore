import { createRouter, createWebHistory } from 'vue-router';
import Login from '@/views/auth/Login.vue';
import Main from '@/views/layouts/Main.vue';

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: Login,
    },
    {
        path: '/',
        name: 'Main',
        component: Main,
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;
