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
}