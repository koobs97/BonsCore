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
}