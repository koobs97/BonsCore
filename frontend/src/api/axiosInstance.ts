/**
 * ========================================
 * 파일명   : axiosInstance.ts
 * ----------------------------------------
 * 설명     : axios 관련 공통 처리 모듈
 * 작성자   : koobonsang
 * 버전     : 1.0
 * 작성일자 : 2025-01-10
 * ========================================
 */

import axios from 'axios';
import { ApiUrls } from './apiUrls';
import { ElLoading, ElMessage, ElMessageBox } from 'element-plus';
import router from '../../router';
import { userStore } from '@/store/userStore';
import { h } from "vue";
import type { Router } from 'vue-router';
import SessionExpiredAlert from "@/components/MessageBox/SessionExpiredAlert.vue";
import {Common} from "@/common/common";

let routerInstance: Router | null = null;

// 라우터 인스턴스를 주입하는 함수
export function setupAxiosInterceptors(router: Router) {
    routerInstance = router;
}

const axiosInstance = axios.create();

// 로그인 관련 화면에서는 에러 alert 안띄우게
// 예시 : 뒤로가기 버튼 -> 로그인 화면 진입 -> 세션이 만료되었습니다. 안뜨게
const publicPaths = ['/login', '/SignUp', '/FindId', '/FindPassword'];

// 인증이 절대로 필요 없는 URL 목록
const NO_AUTH_URLS = [
    '/api/auth/login',
    '/api/auth/get-public-key', // 공개키 요청
    '/api/auth/refresh-token'   // 리프레시 토큰 요청 등
];
// 요청 인터셉터 설정
axiosInstance.interceptors.request.use(
    (config) => {
        // 현재 요청 URL이 NO_AUTH_URLS 목록에 포함되어 있는지 확인
        if (config.url && NO_AUTH_URLS.includes(config.url)) {
            // 목록에 있다면, 토큰을 강제로 제거 (혹시 남아있을 경우를 대비)
            delete config.headers.Authorization;
        } else {
            // 목록에 없다면(logout 포함), sessionStorage에 있는 accessToken을 추가
            const accessToken = sessionStorage.getItem('accessToken');
            if (accessToken) {
                config.headers.Authorization = `Bearer ${accessToken}`;
            }
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// 응답 인터셉터 (403 처리)
axiosInstance.interceptors.response.use(
    response => response,
    async error => {
        const originalRequest = error.config;

        // 403 - FORBIDDEN
        if (error.response && error.response.status === 403 && !originalRequest._retry) {

            // 403-메소드 접근제어
            if(error.response.data.code === 'ER_005') {
                // 메시지 박스 호출
                await Common.customConfirm(
                    '접근제어',
                    error.response.data.message,
                    '확인',
                    '취소',
                    '463px',
                    'warning'
                );
            }

            originalRequest._retry = true;

            // 현재 경로가 publicPaths에 포함되면 토큰 갱신 시도 자체를 건너뜀 =====
            // (public 페이지에서 굳이 유효하지 않은 토큰을 갱신할 필요가 없음)
            if (publicPaths.includes(router.currentRoute.value.path)) {
                return Promise.reject(error);
            }

            try {
                // Refresh Token으로 새 Access Token 요청
                const refreshResponse = await axios.post('/api/auth/refresh', {}, { withCredentials: true });
                const newToken = refreshResponse.data.token;

                // 새 토큰 저장하고 원래 요청 재시도
                sessionStorage.setItem('token', newToken);
                originalRequest.headers.Authorization = `Bearer ${newToken}`;

                return axiosInstance(originalRequest);
            } catch (refreshError) {

                sessionStorage.removeItem('token');
                if(error.response.data.message) {
                    ElMessage.error(error.response.data.message);
                }

                await router.push('/login');
                return Promise.reject(refreshError);
            }

        }
        // 401 - UNAUTHORIZED
        else if(error.response && error.response.status === 401 && !publicPaths.includes(router.currentRoute.value.path)) {

            // 로그인 페이지로 이동하는 함수 선언
            const redirectToLogin = async () => {
                if(router.currentRoute.value.path !== '/login') {
                    await router.push("/login");
                    window.location.reload();
                }
            };

            // 1. SessionExpiredAlert 컴포넌트의 props 타입을 가져옵니다.
            type AlertProps = InstanceType<typeof SessionExpiredAlert>['$props'];

            let alertProps: any = {};

            // 1-1. 중복 로그인 감지 시
            if(error.response.data.data.code === 'ER_104') {
                const customMessage = error.response.data.data.message;

                // 2-1. props 객체를 타입과 함께 별도의 변수로 선언합니다.
                alertProps = {
                    initialSeconds: 10,
                    onComplete: () => {
                        redirectToLogin();
                    },
                    message: customMessage
                };
            }
            else {
                // 2-2. props 객체를 타입과 함께 별도의 변수로 선언합니다.
                alertProps = {
                    initialSeconds: 10,
                    onComplete: () => {
                        redirectToLogin();
                    },
                };
            }

            // 메시지 박스 호출
            const messageBoxPromise = ElMessageBox.alert(
                h(SessionExpiredAlert, alertProps),
                '',
                {
                    confirmButtonText: '즉시 로그아웃',

                    // --- 버튼 가운데 정렬을 위한 옵션 ---
                    center: false,

                    type: '',
                    showClose: false,
                    closeOnClickModal: false,
                    closeOnPressEscape: false,
                }
            ).catch(() => {});

            messageBoxPromise.then(() => {
                // '확인' 버튼을 누르거나, 코드로 close()가 호출되어 성공적으로 닫혔을 때 실행
                redirectToLogin();
            }).catch(() => {
                console.log('MessageBox가 비정상적으로 닫혔습니다.');
                redirectToLogin(); // 어떤 경우든 로그인 페이지로 이동하도록 보장
            });

            return false;
        }
        // 401, 403 에러가 아닌 경우 post에서 처리하도록 던짐
        else {
            return Promise.reject(error);
        }
    }
);

export class Api {

    /**
     * Axios Transaction post
     * @param url           // backend url
     * @param params        // 보낼 파라미터
     * @param loadingOption // loading 옵션 사용 여부
     * @returns
     */
    public static post = async (url: ApiUrls, params: Object, loadingOption?: Boolean): Promise<any> => {
        let loading: any;
        if(loadingOption) {
            loading = ElLoading.service({
                lock: true,
                text: 'Loading',
                background: 'rgba(0, 0, 0, 0.7)',
            })
        }

        try {

            const retData = await axiosInstance.post(url, params)
            const returnData = retData.data

            if(loadingOption) loading.close();

            return returnData
        } catch (error: any) {
            if(loadingOption) loading.close();
            
            // 401, 403 에러는 인터셉터에서 처리
            if(error.response.status !== 401 && error.response.status !== 403) {
                // 에러 response message 출력Error
                if(error.response.data.message) {
                    ElMessage.error(error.response.data.message);
                }

                // CORS는 서버에 도달하기 전에 에러내용이 출력됨, 따라서 data부의 message가 없음
                else if (error.response.data?.includes('CORS')) {
                    ElMessage.error("서버와 연결할 수 없습니다");
                }
            }
            
            // 에러로그
            console.error(error);

            throw(error);
        }

    }

    /**
     * axios GET 메소드 정의
     * @param url
     * @param params
     * @param loadingOption
     */
    public static get = async (url: ApiUrls, params?: Object, loadingOption?: Boolean): Promise<any> => {
        if (loadingOption) {
            const loading = ElLoading.service({
                lock: true,
                text: 'Loading',
                background: 'rgba(0, 0, 0, 0.7)',
            });

            try {
                const retData = await axiosInstance.get(url, { params }); // params를 URL의 query로 변환
                loading.close();
                return retData;
            } catch (error) {
                loading.close();
                return Promise.reject(error);
            }
        } else {
            try {
                return await axios.get(url, { params });
            } catch (error) {
                return Promise.reject(error);
            }
        }
    };

}
