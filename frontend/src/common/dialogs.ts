/**
 * ========================================
 * 파일명   : Dialog.ts
 * ----------------------------------------
 * 설명     : 다이얼로그 공통 관리 ts
 * 작성자   : koobonsang
 * 버전     : 1.0
 * 작성일자 : 2025-10-22
 * ========================================
 */
import { ElMessageBox, ElIcon, ElMessage } from "element-plus";
import { h, nextTick } from "vue";
import { Monitor, ZoomIn } from '@element-plus/icons-vue';
import CustomConfirm from "@/components/MessageBox/CustomConfirm.vue";
import CustomWarning from "@/components/MessageBox/CustomWarning.vue";
import DuplicationLoginConfirm from "@/components/MessageBox/DuplicationLoginConfirm.vue";
import DormantAccountNotice from "@/components/MessageBox/DormantAccountNotice.vue";
import LogOutConfirm from "@/components/MessageBox/LogOutConfirm.vue";
import shieldIcon from '@/assets/images/shield_icon.png';
import { loadRecaptchaScript } from './recaptchaLoader';
import i18n from '@/i18n';

export class Dialogs {

    /**
     * 로그아웃 확인 다이얼로그를 표시.
     * @returns {Promise<string>} ElMessageBox.confirm의 Promise 객체
     */
    public static showLogoutConfirm = () => {
        // 1. 다이얼로그가 호출될 때 스타일을 생성하고 삽입합니다.
        const style = document.createElement('style');
        style.innerHTML = `
        .logout-confirm-box.el-message-box {
            width: 470px !important;
            max-width: none !important;
        }
        
        .el-message-box__header {
            padding-bottom: 0;
        }
    `;
        document.head.appendChild(style);

        // 2. ElMessageBox의 Promise를 변수에 저장합니다. (callback 옵션은 여전히 제거해야 합니다!)
        const confirmPromise = ElMessageBox.confirm(
            h(LogOutConfirm),
            '',
            {
                confirmButtonText: '로그아웃',
                cancelButtonText: '취소',
                customClass: 'logout-confirm-box',
                type: '',
                showClose: false,
            }
        );

        // 3. ★★★ 핵심 ★★★
        // Promise에 .finally()를 체이닝하여 다이얼로그가 닫힐 때(성공/실패 무관)
        // 스타일을 제거하는 작업을 예약하고, 그 Promise를 반환합니다.
        return confirmPromise.finally(() => {
            // 이 코드는 사용자가 '확인'을 누르든 '취소'를 누르든 항상 실행됩니다.
            if (document.head.contains(style)) {
                document.head.removeChild(style);
            }
        });
    }

    /**
     * 중복 로그인 확인 다이얼로그를 표시
     *
     * @param message - 서버로부터 받은 동적 메시지
     * @returns Promise<string> - ElMessageBox.confirm의 Promise 객체
     */
    public static showDuplicateLoginConfirm = (message: string) => {

        const { t, locale } = i18n.global;
        const currentLocale = locale.value;
        const dynamicCustomClass = currentLocale === 'en'
            ? 'custom-message-box custom-message-box-en'
            : 'custom-message-box custom-message-box-ko';

        return ElMessageBox.confirm(
            // message 옵션: 렌더링할 Vue 컴포넌트와 props 전달
            h(DuplicationLoginConfirm, {
                title: t('login.dialogs.duplicateLogin.title'),
                message: message, // 서버에서 받은 메시지를 prop으로 전달
            }),
            // title 옵션
            '',
            // options 객체: 다이얼로그의 세부 동작과 스타일 정의
            {
                confirmButtonText: t('login.dialogs.buttons.login'),
                cancelButtonText: t('login.dialogs.buttons.cancel'),
                customClass: dynamicCustomClass,
                showClose: false,
                distinguishCancelAndClose: true, // 사용자가 ESC나 닫기 버튼으로 닫았는지 '취소'와 구분
                type: '', // 기본 아이콘(예: warning)을 숨기기 위해 빈 값으로 설정
            }
        );
    }

    /**
     * 휴먼계정 안내를 띄워주는 다이얼로그를 표시
     *
     * @param header - 다이얼로그 헤더
     * @param message - 서버로부터 받은 동적 메시지
     * @returns Promise<string> - ElMessageBox.confirm의 Promise 객체
     */
    public static showDormantAccountNotice = (header: string, message: string) => {

        const { t, locale } = i18n.global;
        const currentLocale = locale.value;
        const dynamicCustomClass = currentLocale === 'en'
            ? 'dormant-account-box-en'
            : 'dormant-account-box-ko';

        return ElMessageBox.confirm(
            h(DormantAccountNotice, {
                title: header,
                message: message
            }),
            '',
            {
                confirmButtonText: t('login.dialogs.buttons.verifyIdentity'),
                cancelButtonText: t('login.dialogs.buttons.close'),
                customClass: dynamicCustomClass,
                showClose: false,
                distinguishCancelAndClose: true,
                type: ''
            }
        );
    }

    /**
     * 권장 해상도 및 브라우저 배율 안내 다이얼로그를 표시.
     * @returns Promise<string> - ElMessageBox.alert의 Promise 객체
     */
    public static showResolutionInfo = () => {
        const { t } = i18n.global;
        return ElMessageBox.alert(
            // h() 함수를 사용하여 아이콘과 텍스트가 포함된 복잡한 구조를 생성
            h('div', { class: 'resolution-info-content' }, [
                h('div', { class: 'info-item' }, [
                    h(ElIcon, { class: 'info-icon', size: 20 }, () => h(Monitor)), // 모니터 아이콘
                    h('div', { class: 'info-text' }, [
                        // 3. 하드코딩된 텍스트를 t() 함수로 변경
                        h('span', { class: 'info-label' }, t('login.dialogs.resolution.optimal')),
                        h('span', { class: 'info-value' }, '1920 x 1080'),
                    ]),
                ]),
                h('div', { class: 'info-item' }, [
                    h(ElIcon, { class: 'info-icon', size: 20 }, () => h(ZoomIn)), // 돋보기(배율) 아이콘
                    h('div', { class: 'info-text' }, [
                        // 3. 하드코딩된 텍스트를 t() 함수로 변경
                        h('span', { class: 'info-label' }, t('login.dialogs.resolution.zoom')),
                        h('span', { class: 'info-value' }, '100%'),
                    ]),
                ]),
            ]),
            t('login.dialogs.resolution.title'),
            {
                confirmButtonText: t('login.dialogs.buttons.ok'),
                center: true,
                customClass: 'resolution-info-box',
                showClose: false,
            }
        )
    }

    /**
     * 커스텀 UI를 사용하는 확인 MessageBox를 표시
     * @param title - 커스텀 컴포넌트에 표시될 제목
     * @param message - 커스텀 컴포넌트에 표시될 메시지
     * @param confirmButtonText - 확인 버튼의 텍스트
     * @param cancelButtonText - 취소 버튼의 텍스트
     * @param width - MessageBox의 전체 너비 (기본값: '490px')
     * @param type - confirm / warning
     * @returns Promise<boolean> - 사용자의 선택 결과 (확인: true, 취소: false)
     */
    public static customConfirm = async (
        title: string,
        message: string,
        confirmButtonText: string,
        cancelButtonText: string,
        width: string = '490px',
        type: string = 'confirm'
    ) => {

        // 고유한 ID와 클래스 이름 생성
        const uniqueId = `dynamic-message-box-${Date.now()}`;
        const styleId = `style-for-${uniqueId}`;

        // 주입할 <style> 엘리먼트 생성
        const style = document.createElement('style');
        style.id = styleId;
        style.innerHTML = `
            .${uniqueId}.el-message-box {
                width: ${width} !important;
                max-width: none !important;
            }
            
            .el-message-box__header {
                padding-bottom: 0;
            }
            
            .${uniqueId} .el-message-box__message {
              width: 100%;
            }
        `;
        // <head>에 스타일 주입
        document.head.appendChild(style);

        await ElMessageBox({
            customClass: uniqueId,

            message: h(type === 'confirm' ? CustomConfirm : CustomWarning, {
                title: title,
                message: message,
            }),

            showConfirmButton: true,
            showCancelButton: type === 'confirm',
            confirmButtonText: confirmButtonText,
            cancelButtonText: cancelButtonText,

            title: '',
            showClose: false,
        });
    }

    /**
     * reCAPTCHA 보안 인증 다이얼로그를 표시.
     * @param initialMessage - 다이얼로그가 열릴 때 표시할 초기 메시지 (예: 로그인 실패 메시지)
     * @returns Promise<string> - 사용자가 인증에 성공하면 reCAPTCHA 토큰을 resolve하고,
     *                          취소하거나 실패하면 reject하는 Promise를 반환.
     */
    public static showRecaptchaDialog = (initialMessage: string): Promise<string> => {
        return new Promise(async (resolve, reject) => {
            const { t } = i18n.global;
            const recaptchaContainerId = 'recaptcha-dialog-widget';

            const vnodeContent = h('div', { class: 'modern-recaptcha-content' }, [
                h('img', {
                    src: shieldIcon,
                    class: 'dialog-icon',
                    alt: t('login.dialogs.recaptcha.altIcon') // alt 텍스트
                }),
                h('h3', { class: 'dialog-title' }, t('login.dialogs.recaptcha.title')),
                h('p', { class: 'dialog-description' }, t('login.dialogs.recaptcha.description1')),
                h('p', { class: 'dialog-description' }, t('login.dialogs.recaptcha.description2')),
                h('div', { id: recaptchaContainerId, class: 'recaptcha-widget-container' })
            ]);

            // await 넣지 말 것
            ElMessageBox.alert(
                vnodeContent, '', {
                showConfirmButton: false,
                showClose: false,
                center: true,
                customClass: 'modern-recaptcha-dialog',
                callback: (action: any) => {
                    if (action === 'cancel' || action === 'close') {
                        ElMessage.info(t('login.dialogs.recaptcha.cancelMessage'));
                        reject('cancelled');
                    }
                }
            });

            try {
                await loadRecaptchaScript();
                await nextTick();
                if (window.grecaptcha && window.grecaptcha.render) {
                    const isDarkMode = document.documentElement.classList.contains('dark');
                    const siteKey = import.meta.env.VITE_RECAPTCHA_SITE_KEY;

                    window.grecaptcha.render(recaptchaContainerId, {
                        'sitekey': siteKey,
                        'theme': isDarkMode ? 'dark' : 'light',
                        'callback': (token: string) => {
                            ElMessageBox.close();
                            ElMessage.success(t('login.dialogs.recaptcha.successMessage'));
                            resolve(token);
                        },
                        'expired-callback': () => {
                            ElMessage.warning(t('login.dialogs.recaptcha.expiredMessage'));
                            ElMessageBox.close();
                            reject('expired');
                        },
                        'error-callback': () => {
                            ElMessage.error(t('login.dialogs.recaptcha.errorMessage'));
                            ElMessageBox.close();
                            reject('error');
                        }
                    } as any);

                    if (initialMessage) {
                        ElMessage.warning(initialMessage);
                    }
                } else {
                    // 이 경우는 거의 발생하지 않아야 하지만, 만약을 위한 방어 코드
                    ElMessage.error(t('login.dialogs.recaptcha.loadFailedMessage'));
                    ElMessageBox.close();
                    reject('load-failed');
                }

            } catch (error) {
                ElMessage.error(t('login.dialogs.recaptcha.loadFailedMessage'));
                ElMessageBox.close();
                reject('load-failed');
            }
        });
    }
}