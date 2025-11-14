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
import { romanize } from "@romanize/korean";
import {userState, userStore} from "@/store/userStore";

type CaseType = "none" | "capitalize" | "capitalizeEach" | "upper";

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
     * 로그인 성공 시 유저정보 세팅
     */
    public static async setUser(): Promise<void> {
        const response = await Api.post(ApiUrls.GET_USER, {}, true);
        let userInfo = response.data as userState
        userInfo.userNameEn = Common.romanizeName(userInfo.userName, "upper");
        sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
        userStore().setUserInfo(userInfo);
    }

    /**
     * 한글로 된 이름을 영문으로 변환
     * @param name
     * @param caseType
     */
    public static romanizeName(name: string, caseType: CaseType = "none"): string {
        let result = romanize(name); // 기본: gubonsang

        switch (caseType) {
            case "capitalize":
                // 첫 글자만 대문자 → Gubonsang
                return result.charAt(0).toUpperCase() + result.slice(1);

            case "capitalizeEach":
                // 단어마다 대문자 → Gu Bonsang (띄어쓰기 필요함)
                return result
                    .split(" ")
                    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
                    .join(" ");

            case "upper":
                // 전체 대문자 → GUBONSANG
                return result.toUpperCase();

            default:
                return result; // 기본 소문자
        }
    }

}