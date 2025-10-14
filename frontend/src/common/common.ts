/**
 * ========================================
 * 파일명   : common.ts
 * ----------------------------------------
 * 설명     : 공통유틸
 * 작성자   : koobonsang
 * 버전     : 1.0
 * 작성일자 : 2025-01-10
 * ========================================
 */
import JSEncrypt from 'jsencrypt';
import { Api } from "@/api/axiosInstance";
import { ApiUrls } from "@/api/apiUrls";
import {ElMessageBox} from "element-plus";
import { h, CSSProperties } from "vue";
import SignUpConfirm from "@/components/MessageBox/SignUpConfirm.vue";

export class Common {

    /**
     * 빈값 검사 함수
     * @param param
     */
    public static isEmpty = (param: any) => {
        return param === undefined || param === null || param.trim() === "";
    }

    /**
     * 비밀번호를 공개키로 암호화
     * @param password
     * @returns 암호화된 문자열을 담은 Promise
     */
    public static async encryptPassword(password: string): Promise<string> {
        try {
            // 서버에서 공개 키 받아오기
            const { data: publicKey } = await Api.get(ApiUrls.GET_PUBLIC_KEY);

            // 공개키가 없거나 비어있으면 에러 처리
            if (!publicKey) {
                console.error("Public key is not available.");
                return ''; // 또는 throw new Error("공개키를 받아올 수 없습니다.");
            }

            const encryptor = new JSEncrypt();
            encryptor.setPublicKey(publicKey);

            const encrypted = encryptor.encrypt(password);

            // 암호화 실패 시 빈 문자열 반환
            return encrypted || '';
        } catch (error) {
            console.error("Password encryption failed:", error);
            return ''; // 예외 발생 시 빈 문자열 반환
        }
    }

    /**
     * 커스텀 UI를 사용하는 확인 MessageBox를 표시
     * @param title - 커스텀 컴포넌트에 표시될 제목
     * @param message - 커스텀 컴포넌트에 표시될 메시지
     * @param confirmButtonText - 확인 버튼의 텍스트
     * @param cancelButtonText - 취소 버튼의 텍스트
     * @param width - MessageBox의 전체 너비 (기본값: '490px')
     * @returns Promise<boolean> - 사용자의 선택 결과 (확인: true, 취소: false)
     */
    public static customConfirm = async (title: string, message: string, confirmButtonText: string, cancelButtonText: string, width: string = '490px') => {

        // 고유한 ID와 클래스 이름 생성
        const uniqueId = `dynamic-message-box-${Date.now()}`;
        const styleId = `style-for-${uniqueId}`;

        // 주입할 <style> 엘리먼트 생성
        const style = document.createElement('style');
        style.id = styleId;
        style.innerHTML = `
            .${uniqueId}.el-message-box {
                width: ${width} !important;
                max-width: none !important; /* 기존 max-width를 무시 */
            }`;
        // <head>에 스타일 주입
        document.head.appendChild(style);

        await ElMessageBox({
            customClass: uniqueId,

            message: h(SignUpConfirm, {
                title: title,
                message: message,
            }),

            showConfirmButton: true,
            showCancelButton: true,
            confirmButtonText: confirmButtonText,
            cancelButtonText: cancelButtonText,

            title: '',
            showClose: false,
        });
    }

}