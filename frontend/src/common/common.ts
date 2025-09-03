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
import {h} from "vue";
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

    public static customConfirm = async (title: string, message: string, confirmButtonText: string, cancelButtonText: string) => {
        await ElMessageBox.confirm(
            // 1. message 옵션에 h() 함수를 사용하여 커스텀 컴포넌트를 렌더링합니다.
            h(SignUpConfirm, {
                // CustomConfirm 컴포넌트에 props 전달
                title: title,
                message: message,
            }),
            // 2. 기본 title은 사용하지 않으므로 빈 문자열로 둡니다.
            '',
            {
                confirmButtonText: confirmButtonText,
                cancelButtonText: cancelButtonText,
                // 3. 커스텀 클래스를 추가하여 세부 스타일을 조정할 수 있습니다.
                customClass: 'custom-message-box',
                // 4. CustomConfirm 컴포넌트가 자체 아이콘과 UI를 가지므로,
                //    MessageBox의 기본 UI 요소들은 비활성화합니다.
                showClose: false, // 오른쪽 위 'X' 닫기 버튼 숨김
                type: '', // 기본 'warning' 아이콘 숨김
            }
        );
    }

}