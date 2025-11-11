import { createI18n } from 'vue-i18n';
import ko from '@/i18n/locales/ko.json';
import en from '@/i18n/locales/en.json';

const i18n = createI18n({
    legacy: false, // Composition API를 사용하려면 false로 설정해야 합니다.
    locale: localStorage.getItem('language') || 'ko', // 저장된 언어를 불러오거나 기본값 'ko' 사용
    fallbackLocale: 'en', // 선택한 언어에 번역이 없을 경우 보여줄 대체 언어
    messages: {
        ko,
        en,
    },
    globalInjection: true,
});

export default i18n;