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
        return ElMessageBox.confirm(
            // message 옵션: 렌더링할 Vue 컴포넌트와 props 전달
            h(DuplicationLoginConfirm, {
                title: '중복 로그인 감지',
                message: message, // 서버에서 받은 메시지를 prop으로 전달
            }),
            // title 옵션
            '',
            // options 객체: 다이얼로그의 세부 동작과 스타일 정의
            {
                confirmButtonText: '로그인',
                cancelButtonText: '취소',
                customClass: 'custom-message-box',
                showClose: false,
                distinguishCancelAndClose: true, // 사용자가 ESC나 닫기 버튼으로 닫았는지 '취소'와 구분
                type: '', // 기본 아이콘(예: warning)을 숨기기 위해 빈 값으로 설정
            }
        );
    }

    /**
     * 휴먼계정 안내를 띄워주는 다이얼로그를 표시
     *
     * @param message - 서버로부터 받은 동적 메시지
     * @returns Promise<string> - ElMessageBox.confirm의 Promise 객체
     */
    public static showDormantAccountNotice = (header: string, message: string) => {
        return ElMessageBox.confirm(
            // message 옵션: 렌더링할 Vue 컴포넌트와 props 전달
            h(DormantAccountNotice, { // 새로 만든 컴포넌트 사용
                title: header,
                message: message // 서버에서 받은 메시지
            }),
            '',
            {
                confirmButtonText: '본인인증 하러가기',
                cancelButtonText: '닫기',
                customClass: 'custom-message-box',
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
        return ElMessageBox.alert(
            // h() 함수를 사용하여 아이콘과 텍스트가 포함된 복잡한 구조를 생성
            h('div', { class: 'resolution-info-content' }, [
                h('div', { class: 'info-item' }, [
                    h(ElIcon, { class: 'info-icon', size: 20 }, () => h(Monitor)), // 모니터 아이콘
                    h('div', { class: 'info-text' }, [
                        h('span', { class: 'info-label' }, '최적 해상도'),
                        h('span', { class: 'info-value' }, '1920 x 1080'),
                    ]),
                ]),
                h('div', { class: 'info-item' }, [
                    h(ElIcon, { class: 'info-icon', size: 20 }, () => h(ZoomIn)), // 돋보기(배율) 아이콘
                    h('div', { class: 'info-text' }, [
                        h('span', { class: 'info-label' }, '브라우저 배율'),
                        h('span', { class: 'info-value' }, '125%'),
                    ]),
                ]),
            ]),
            '권장 사용 환경', // Title
            {
                confirmButtonText: '확인',
                center: true,
                customClass: 'resolution-info-box', // 커스텀 스타일 클래스
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
        return new Promise((resolve, reject) => {
            const recaptchaContainerId = 'recaptcha-dialog-widget';

            // h 함수를 사용하여 VNode 생성
            const vnodeContent = h('div', { class: 'modern-recaptcha-content' }, [
                h('img', { src: shieldIcon, class: 'dialog-icon', alt: '보안 방패 아이콘' }),
                h('h3', { class: 'dialog-title' }, '보안 인증'),
                h('p', { class: 'dialog-description' }, '계정 보안을 위해 reCAPTCHA 인증이 필요합니다.'),
                h('p', { class: 'dialog-description' }, '안전한 로그인을 위해, 로봇이 아님을 증명해주세요.'),
                h('div', { id: recaptchaContainerId, class: 'recaptcha-widget-container' })
            ]);

            ElMessageBox.alert(vnodeContent, '', {
                showConfirmButton: false,
                showClose: false, // 사용자가 닫지 못하도록 설정
                center: true,
                customClass: 'modern-recaptcha-dialog',
                callback: (action: any) => {
                    if (action === 'cancel' || action === 'close') {
                        ElMessage.info('보안 인증이 취소되었습니다.');
                        reject('cancelled');
                    }
                }
            });

            nextTick(() => {
                if (window.grecaptcha && window.grecaptcha.render) {
                    const isDarkMode = document.documentElement.classList.contains('dark');

                    window.grecaptcha.render(recaptchaContainerId, {
                        'sitekey': '6LdRdgEsAAAAAEE7-VXh1Amykn3TI5AtNq8xZAWr',
                        'theme': isDarkMode ? 'dark' : 'light',
                        'callback': (token: string) => {
                            ElMessageBox.close();
                            ElMessage.success('보안 인증이 완료되었습니다.');
                            resolve(token); // 성공 시 토큰과 함께 Promise를 resolve
                        },
                        'expired-callback': () => {
                            ElMessage.warning('보안 인증이 만료되었습니다. 다시 시도해주세요.');
                            // 다이얼로그를 닫고 다시 열도록 유도하거나, 현재 창에서 reCAPTCHA를 리셋할 수 있음
                            ElMessageBox.close();
                            reject('expired');
                        },
                        'error-callback': () => {
                            ElMessage.error('보안 인증 중 오류가 발생했습니다.');
                            ElMessageBox.close();
                            reject('error');
                        }
                    } as any);

                    // 로그인 실패 후 에러메시지 표시
                    if (initialMessage) {
                        ElMessage.warning(initialMessage);
                    }

                } else {
                    ElMessage.error('보안 인증 모듈을 로드할 수 없습니다.');
                    ElMessageBox.close();
                    reject('load-failed');
                }
            });
        });
    }
}