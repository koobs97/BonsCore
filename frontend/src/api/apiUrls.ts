export enum ApiUrls {

    GET_PUBLIC_KEY = "/api/public-key",
    GET_MESSAGES = '/api/common/messages',

    // auth
    LOGIN = '/api/auth/login',
    LOGOUT = '/api/auth/logout',
    CHECK_ID = '/api/auth/check/id',
    CHECK_EMAIL = '/api/auth/check/email',
    CHECK_PWNED_PASSWORD = '/api/auth/check-pwned-password',
    SIGN_UP = '/api/auth/signup',
    SEND_MAIL = '/api/auth/email/verification',
    CHECK_CODE = '/api/auth/email/verification/confirm',
    COPY_ID = '/api/auth/copy-id',
    GET_USER_PASSWORD_HINT = '/api/auth/hint',
    VALIDATE_PASSWORD_HINT = '/api/auth/hint/validate',
    UPDATE_PASSWORD = '/api/auth/password',

    // users
    GET_USER = '/api/users/me',
    ACTIVATE_DORMANT = '/api/users/me/dormant',

    // authorization
    GET_MENUS = '/api/authorization/menus',
    GET_USER_INFOS = '/api/authorization/users',
    GET_ACTIVITY = '/api/authorization/logs/activity-codes',
    GET_LOGS = '/api/authorization/logs',
    UPDATE_USER_INFO = '/api/authorization/users',
    VALIDATE_PASSWORD = '/api/authorization/users/password/validate',
    UPDATE_PASSWORD_AF_LOGIN = '/api/authorization/users/password',
    GET_PASSWORD_HINT = '/api/authorization/security-questions',
    UPDATE_PASSWORD_HINT = '/api/authorization/users/hint',
    WITHDRAWN = '/api/authorization/users/me',

    // messages
    GET_MESSAGES_MANAGE = '/api/messages',
    SAVE_MESSAGES = '/api/messages',
    DELETE_MESSAGE = '/api/messages',

    // analysis
    RANDOM_RECOMMENDATIONS = '/api/analysis/random-recommendations',
    NAVER_STORE_SEARCH = '/api/analysis/stores',
    NAVER_BLOG_SEARCH = '/api/analysis/details',
    WEATHER_SEARCH = '/api/analysis/weather',
    HOLIDAY_INFO = '/api/analysis/holiday-status',
    SEARCH_TREND = '/api/analysis/search-trend',
    OPENING_INFO = '/api/analysis/opening-info',
    SURROUNDING_INFO = '/api/analysis/surroundings',

    // gourmet-records
    CREATE_GOURMET_RECORD = '/api/gourmet-records',
    GET_GOURMET_RECORDS = '/api/gourmet-records',
    CLEAR_TEMP_FILE = '/api/gourmet-records/temp-files',

    // files
    FILE_UPLOAD = '/api/files/upload',

    // sample
    PAGING = '/api/sample/paging',
    MASKING = '/api/sample/masking',
    TABLE_TEST = '/api/sample/tableTest',
}
