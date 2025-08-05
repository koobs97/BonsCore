export enum ApiUrls {

    GET_PUBLIC_KEY = "/api/public-key",

    // auth
    LOGIN = '/api/auth/login',
    LOGOUT = '/api/auth/logout',
    CHECK_ID = '/api/auth/isDuplicateId',
    CHECK_EMAIL = '/api/auth/isDuplicateEmail',
    SIGN_UP = '/api/auth/signup',
    SEND_MAIL = '/api/auth/sendmail',
    CHECK_CODE = '/api/auth/verify-email',
    COPY_ID = '/api/auth/copy-id',
    UPDATE_PASSWORD = '/api/auth/update-password',

    // users
    GET_USER = '/api/users/me',

    GET_MENUS = '/api/authorization/getMenus',
    GET_LOGS = '/api/authorization/getLogs',

    // biz
    PAGING = '/api/sample/paging',
    MASKING = '/api/sample/masking',
    TABLE_TEST = '/api/sample/tableTest',

}