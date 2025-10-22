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
import { ElMessageBox, ElIcon } from "element-plus";
import { h } from "vue";
import { Monitor, ZoomIn } from '@element-plus/icons-vue';
import CustomConfirm from "@/components/MessageBox/CustomConfirm.vue";
import CustomWarning from "@/components/MessageBox/CustomWarning.vue";
import DuplicationLoginConfirm from "@/components/MessageBox/DuplicationLoginConfirm.vue";
import DormantAccountNotice from "@/components/MessageBox/DormantAccountNotice.vue";

export class Dialogs {

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
    public static showDormantAccountNotice = (message: string) => {
        return ElMessageBox.confirm(
            // message 옵션: 렌더링할 Vue 컴포넌트와 props 전달
            h(DormantAccountNotice, { // 새로 만든 컴포넌트 사용
                title: '휴면 계정 안내',
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
}