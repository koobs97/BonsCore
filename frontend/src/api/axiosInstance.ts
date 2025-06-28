import axios from 'axios';
import { ApiUrls } from './apiUrls';
import {ElLoading, ElMessageBox} from 'element-plus';
import router from '../../router';
import { ElMessage } from 'element-plus'
import { userStore } from '@/store/userStore';
import { h } from "vue";
import type { Router } from 'vue-router';
import SessionExpiredAlert from "@/components/MessageBox/SessionExpiredAlert.vue";

let routerInstance: Router | null = null;

// 라우터 인스턴스를 주입하는 함수
export function setupAxiosInterceptors(router: Router) {
    routerInstance = router;
}

const axiosInstance = axios.create();

// 요청 인터셉터 설정
axiosInstance.interceptors.request.use(
    (config) => {
        const noAuthUrls = ['/api/auth/login', '/api/auth/refresh'];
        if (config.url && noAuthUrls.includes(config.url)) {
            // 로그인 요청은 토큰을 추가하지 않도록 설정
            delete config.headers.Authorization;
        } else {
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
            originalRequest._retry = true;

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
        if(error.response && error.response.status === 401) {

            // 로그인 페이지로 이동하는 로직
            const redirectToLogin = async () => {
                userStore().delUserInfo();
                if(router.currentRoute.value.path !== '/login') {
                    await router.push("/login");
                    window.location.reload();
                }
            };

            // 1. SessionExpiredAlert 컴포넌트의 props 타입을 가져옵니다.
            type AlertProps = InstanceType<typeof SessionExpiredAlert>['$props'];

            // 2. props 객체를 타입과 함께 별도의 변수로 선언합니다.
            const alertProps: AlertProps = {
                initialSeconds: 10,
                onComplete: () => {
                    redirectToLogin();
                }
            };

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

        // 다른 상태 코드 처리 가능
        // if (status === 401) { ... }

        return Promise.reject(error);
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

            // 에러 내용 출력
            console.error('❗API Error Response -> {}', error);
            console.error('❗API Error Response -> {}', error.response.data);

            // 에러 response message 출력Error
            if(error.response.data.message) {
                ElMessage.error(error.response.data.message);
            }
            // CORS는 서버에 도달하기 전에 에러내용이 출력됨, 따라서 data부의 message가 없음
            else if (error.response.data?.includes('CORS')) {
                ElMessage.error("서버와 연결할 수 없습니다");
            }

            return error.response;
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
