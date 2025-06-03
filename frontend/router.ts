import { createRouter, createWebHistory } from 'vue-router';

import indexHome from './src/views/indexHome.vue';
import { userStore } from '@/store/userStore';

// 페이지 경로를 저장할 배열
const routes = [];

// views와 components 폴더 내 모든 .vue 파일을 동적 가져오기
const viewPages = import.meta.glob('./src/views/**/*.vue');
const componentPages = import.meta.glob('./src/components/*.vue');

// 두 개의 모듈을 합침
const allPages = { ...viewPages, ...componentPages };

// 모든 파일을 순회하며 라우터 설정 추가
for (const path in allPages) {
    // 라우터 경로로 사용할 path 생성
    const routePath = path
        .replace(/^\.\/src\/views/, '')      // views 폴더 경로 제거
        .replace(/^\.\/src\/components/, '') // components 폴더 경로 제거
        .replace(/\.vue$/, '');              // .vue 확장자 제거

    routes.push({
        path: '/' + routePath,
        component: allPages[path] // 동적 import는 비동기 컴포넌트로 사용 가능
    });
}

routes.push({
    path: '/',
    component: indexHome,
    beforeEnter: async (to: any, from: any, next: any) => {
        const isLoggedIn = userStore().isLoggedIn;
        if (isLoggedIn) {
            next();           // 로그인 상태면 계속 진행
        } else {
            next('/login');   // 로그인되어 있지 않으면 로그인 페이지로 리다이렉트
        }
    },
});

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;