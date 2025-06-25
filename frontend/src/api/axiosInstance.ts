import axios from 'axios';
import { ApiUrls } from './apiUrls';
import { ElLoading } from 'element-plus';
import router from '../../router';
import { ElMessage } from 'element-plus'
import { userStore } from '@/store/userStore';
import { nextTick } from "vue";

const axiosInstance = axios.create();

// 요청 인터셉터 설정
axiosInstance.interceptors.request.use(
    (config) => {
        if (config.url && config.url.startsWith('/api/auth/')) {
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

            // 다른 상태 코드 처리 가능
            // if (status === 401) { ... }
        }

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

        if(loadingOption) {
            const loading = ElLoading.service({
                lock: true,
                text: 'Loading',
                background: 'rgba(0, 0, 0, 0.7)',
            })

            try {

                const retData = await axiosInstance.post(url, params)
                const returnData = retData.data

                loading.close()

                return returnData
            } catch (error) {
                loading.close();

                // 타입에러에 따른 에러 정의
                const Error = error as any;

                // 에러 내용 출력
                console.error('❗API Error Response:', Error);
                console.error('❗API Error Response:', Error.response.data);

                // 에러 response message 출력Error
                if(Error.response.data.message) {
                    ElMessage.error(Error.response.data.message);
                }
                // CORS는 서버에 도달하기 전에 에러내용이 출력됨, 따라서 data부의 message가 없음
                else if (Error.response.data?.includes('CORS')) {
                    ElMessage.error("서버와 연결할 수 없습니다");
                }

                // Unauthorized
                if(Error.status === 401) {
                    ElMessage.error("세션만료");
                    await new Promise(resolve => setTimeout(resolve, 200)); // 0.2초 대기

                    ElMessage.error("로그인 화면으로 이동합니다.");
                    await new Promise(resolve => setTimeout(resolve, 200)); // 0.2초 대기

                    userStore().delUserInfo();
                    await nextTick();

                    // 인증실패 시 로그인 화면으로 이동
                    if(router.currentRoute.value.path !== '/login') {
                        await router.push("/login");
                        window.location.reload();
                    }

                    return false;
                }

                return Error.response;
            }
        }
        else {
            try {
                return await axios.post(url, params)
            } catch (error) {
                return Promise.reject(error)
            }
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
