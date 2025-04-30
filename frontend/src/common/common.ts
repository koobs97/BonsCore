
export class Common {

    /**
     * 빈값 검사 함수
     * @param param
     */
    public static isEmpty = (param: any) => {
        return param === undefined || param.trim() === "";
    }
}