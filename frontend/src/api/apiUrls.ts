export enum ApiUrls {
    GET_PUBLIC_KEY = "/api/public-key",
    LOGIN = '/api/auth/login',
    LOGOUT = '/api/auth/logout',
    USER_INFO = '/api/user/info',
    REFRESH_TOKEN = '/api/auth/refresh',
    CHECK_ID = '/api/auth/isDuplicateId',
    CHECK_EMAIL = '/api/auth/isDuplicateEmail',
    SIGN_UP = '/api/auth/signup',
    SEND_MAIL = '/api/auth/sendmail',
    CHECK_CODE = '/api/auth/verify-email',
    COPY_ID = '/api/auth/copy-id',

    // 필요에 따라 API 추가
    AF_LOGIN = '/api/aflogin/afterLogin',
    GET_USER = '/api/aflogin/me',


    PAGING = '/api/sample/paging',
    MASKING = '/api/sample/masking',
    TABLE_TEST = '/api/sample/tableTest',

}