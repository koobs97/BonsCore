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
    GET_ACTIVITY = '/api/authorization/geActivityCds',
    GET_LOGS = '/api/authorization/getLogs',

    // biz
    PAGING = '/api/sample/paging',
    MASKING = '/api/sample/masking',
    TABLE_TEST = '/api/sample/tableTest',

    RANDOM_RECOMMENDATIONS = '/api/analysis/random-recommendations',

    //naver api
    NAVER_STORE_SEARCH = '/api/analysis/stores',
    NAVER_BLOG_SEARCH = '/api/analysis/details',

    WEATHER_SEARCH = '/api/analysis/weather',
    HOLIDAY_INFO = '/api/analysis/holiday-status',
    SEARCH_TREND = '/api/analysis/search-trend',
    OPENING_INFO = '/api/analysis/openingInfo',
    SURROUNDING_INFO = '/api/analysis/surroundings',

    UPDATE_USER_INFO = '/api/authorization/updateUserInfo',
    VALIDATE_PASSWORD = '/api/authorization/validatePassword',
    UPDATE_PASSWORD_AF_LOGIN = '/api/authorization/updatePassword'

}