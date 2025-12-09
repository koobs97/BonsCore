import { createI18n } from 'vue-i18n';
import { Api } from '@/api/axiosInstance';
import { ApiUrls } from '@/api/apiUrls';

const i18n = createI18n({
    legacy: false, // Composition API를 사용하려면 false로 설정해야 합니다.
    locale: localStorage.getItem('language') || 'ko', // 저장된 언어를 불러오거나 기본값 'ko' 사용
    fallbackLocale: 'en', // 선택한 언어에 번역이 없을 경우 보여줄 대체 언어
    globalInjection: true,
    messages: {
        ko: {}, // 초기에는 빈 객체 혹은 필수 메시지만 포함
        en: {}
    },
});

/**
 * 2. 언어 변경 및 데이터 로딩 함수 (핵심 로직)
 * @param lang 변경할 언어 코드 ('ko', 'en')
 */
export async function loadLanguageAsync(lang: string) {
    // 이미 로딩된 언어라면 API 호출 없이 언어만 변경
    // (메시지가 비어있지 않은지 확인)
    const currentMessages = i18n.global.getLocaleMessage(lang);
    if (Object.keys(currentMessages).length > 0) {
        console.log(`[i18n] ${lang} 이미 로드됨.`);
        setI18nLanguage(lang);
        return lang;
    }

    try {
        console.log(`[i18n] ${lang} 데이터 요청 중...`);
        // 3. DB에서 해당 언어 데이터 조회 (API 호출)
        const response = await Api.get(ApiUrls.GET_MESSAGES, { lang: lang }, true);
        const messageData = response.data.data;

        console.log(`[i18n] 서버 응답 데이터(${lang}):`, messageData);

        if (!messageData || Object.keys(messageData).length === 0) {
            console.warn(`[i18n] 경고: ${lang} 데이터가 비어있습니다!`);
        }

        // 4. 받아온 데이터를 i18n에 주입
        i18n.global.setLocaleMessage(lang, messageData);

        // 5. 언어 변경 적용
        setI18nLanguage(lang);

        return lang;
    } catch (error) {
        console.error(`Failed to load language: ${lang}`, error);
        return Promise.reject(error);
    }
}

/**
 * 내부적으로 언어 상태 및 설정을 업데이트하는 헬퍼 함수
 */
function setI18nLanguage(lang: any) {
    i18n.global.locale.value = lang;

    // HTML lang 속성 변경
    document.querySelector('html')?.setAttribute('lang', lang);

    // LocalStorage 저장
    localStorage.setItem('language', lang);
}

export default i18n;