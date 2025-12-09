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
import {ApiUrls} from './apiUrls';
import {ElLoading, ElMessage, ElMessageBox} from 'element-plus';
import router from '../../router';
import {h} from "vue";
import type {Router} from 'vue-router';
import SessionExpiredAlert from "@/components/MessageBox/SessionExpiredAlert.vue";
import {Dialogs} from "@/common/dialogs";
import i18n from '@/i18n';
import DuplicateLoginDialog from "@/components/MessageBox/DuplicateLoginDialog.vue";
import {userStore} from "@/store/userStore";

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
    '/api/auth/refresh-token',  // 리프레시 토큰 요청 등
    '/api/common/messages'      // 메시지조회
];
// 요청 인터셉터 설정
axiosInstance.interceptors.request.use(
    (config) => {
        // 현재 요청 URL이 NO_AUTH_URLS 목록에 포함되어 있는지 확인
        if (config.url && NO_AUTH_URLS.some(url => config.url?.includes(url))) {
            // 목록에 있다면, 토큰을 강제로 제거 (혹시 남아있을 경우를 대비)
            delete config.headers.Authorization;
        } else {
            // 목록에 없다면(logout 포함), sessionStorage에 있는 accessToken을 추가
            const accessToken = sessionStorage.getItem('accessToken');
            if (accessToken) {
                config.headers.Authorization = `Bearer ${accessToken}`;
            }
        }

        // 헤더에 언어설정 추가
        config.headers['Accept-Language'] = i18n.global.locale.value;

        return config;
    },
    (error) => Promise.reject(error)
);

// 응답 인터셉터 (403 처리)
axiosInstance.interceptors.response.use(
    response => response,
    async error => {

        const originalRequest = error.config;
        const { t } = i18n.global;

        // 403 - FORBIDDEN
        if (error.response && error.response.status === 403 && !originalRequest._retry) {

            // ACCESS_DENIED-메소드 접근제어
            if(error.response.data.code === 'ER_105') {
                // 메시지 박스 호출
                await Dialogs.customConfirm(
                    '접근제어',
                    error.response.data.message,
                    '확인',
                    '취소',
                    '463px',
                    'error'
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
                    ElMessage({ message: error.response.data.message, grouping: true, type: 'error' })
                }

                await router.push('/login');
                return Promise.reject(refreshError);
            }

        }
        // 401 - UNAUTHORIZED
        else if(error.response && error.response.status === 401) {

            // ElMessageBox 인스턴스를 닫는 함수
            const closeMessageBox = () => {
                ElMessageBox.close();
            };

            // 로그인 페이지로 이동하는 함수
            const redirectToLogin = async () => {
                if(router.currentRoute.value.path !== '/login') {
                    try {
                        await Api.post(ApiUrls.LOGOUT, {}, false);
                    } catch (error) {}

                    userStore().delUserInfo();
                    sessionStorage.clear();

                    window.location.href = "/login";
                }
            };

            // 비밀번호 변경 페이지로 이동하는 함수
            const redirectToChangePassword = async () => {
                closeMessageBox();
                await router.push("/ChangePassword");
                window.location.reload();
            };

            let alertProps: any = {};

            // 1-1. 중복 로그인 감지 시
            if(error.response.data.data.code === 'ER_104') {
                const { locale } = i18n.global;
                const currentLocale = locale.value;
                const dynamicCustomClass = currentLocale === 'en'
                    ? 'duplicate-login-box-en'
                    : 'duplicate-login-box-ko';

                ElMessageBox({
                    title: ' ', // 제목은 컴포넌트 내부에서 처리하므로 비워둠
                    message: h(DuplicateLoginDialog, {
                        onGoToLogin: redirectToLogin,
                        onGoToChangePassword: redirectToChangePassword,
                    }),
                    customClass: dynamicCustomClass,
                    showConfirmButton: false,
                    showCancelButton: false,
                    type: '',
                    showClose: false,
                    closeOnClickModal: false,
                    closeOnPressEscape: false,
                }).catch(() => {
                    // 비정상적으로 닫혔을 때 (거의 발생하지 않음)
                    redirectToLogin();
                });

                return Promise.reject(error);
            }
            // ACCOUNT_LOCKED-메소드 접근제어
            else if(error.response.data.data.code === 'ER_106') {
                // 메시지 박스 호출
                await Dialogs.customConfirm(
                    '계정잠김',
                    error.response.data.message || '로그인 시도 횟수 초과로 계정이 잠겼습니다.',
                    '확인',
                    '취소',
                    '493px',
                    'error'
                );

                return false;
            }
            else {
                // 2-2. props 객체를 타입과 함께 별도의 변수로 선언합니다.
                alertProps = {
                    initialSeconds: 10,
                    onComplete: () => {
                        redirectToLogin();
                    },
                };

                // 메시지 박스 호출
                const messageBoxPromise = ElMessageBox.alert(
                    h(SessionExpiredAlert, alertProps),
                    '',
                    {
                        confirmButtonText: t('session.expired.confirmButtonText'),

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
        }
        // 401, 403 에러가 아닌 경우 post에서 처리하도록 던짐
        else {
            return Promise.reject(error);
        }
    }
);

interface PostOptions {
    loading?: boolean;
    headers?: Record<string, string>; // 헤더 객체 타입 정의
}

export class Api {

    /**
     * Axios Transaction post
     * @param url           // backend url
     * @param params        // 보낼 파라미터
     * @param loadingOption // loading 옵션 사용 여부
     * @param options       // header 옵션
     * @returns
     */
    public static post = async (url: ApiUrls, params: Object, loadingOption?: Boolean, options: PostOptions = {}): Promise<any> => {
        let loading: any;
        if(loadingOption) {
            loading = ElLoading.service({
                lock: true,
                text: 'Loading',
                background: 'rgba(0, 0, 0, 0.7)',
            })
        }
        try {
            const retData = await axiosInstance.post(url, params, { headers: options.headers })
            const returnData = retData.data
            if(loadingOption) loading.close();
            return returnData
        } catch (error: any) {
            if(loadingOption) loading.close();
            
            // 401, 403 에러는 인터셉터에서 처리
            if(error.response.status !== 401 && error.response.status !== 403) {

                // EX_001 -> 가게 추천 서비스 이용 불가 시에는 alert 제외
                if(error.response.data.header.code === "EX_001")
                    throw(error);

                let errorCodeFromBackend: string | undefined;
                let backendMessage: string | undefined;

                // 1. ApiResponse 구조에서 header.code를 먼저 시도
                if (error.response.data && error.response.data.header && error.response.data.header.code) {
                    errorCodeFromBackend = error.response.data.header.code;
                    backendMessage = error.response.data.message;
                }
                // 2. LoginResponseDto와 같이 응답 DTO 자체가 code 필드를 가질 경우
                else if (error.response.data && error.response.data.code) {
                    errorCodeFromBackend = error.response.data.code;
                    backendMessage = error.response.data.message;
                }
                // 3. 그 외 경우 (메시지만 있는 경우 등)
                else if (error.response.data && error.response.data.message) {
                    backendMessage = error.response.data.message;
                }

                let messageToShow = backendMessage; // 기본값은 백엔드에서 받은 메시지

                // 에러 코드가 있다면 i18n 번역을 시도
                if (errorCodeFromBackend) {
                    const translationKey = `errors.${errorCodeFromBackend}`; // 예: "errors.ER_005"
                    const { t, te } = i18n.global;

                    if (te(translationKey)) {
                        messageToShow = t(translationKey); // 번역된 메시지 사용
                    }
                }
                // CORS는 서버에 도달하기 전에 에러내용이 출력됨, 따라서 data부의 message가 없음
                else if (error.response.data?.includes('CORS')) {
                    messageToShow = "서버와 연결할 수 없습니다"; // 클라이언트 자체 정의 에러
                }

                if (messageToShow) {
                    ElMessage({ message: messageToShow, grouping: true, type: 'error' });
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
