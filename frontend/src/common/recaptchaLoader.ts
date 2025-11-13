// common/recaptchaLoader.ts

import i18n from '@/i18n';

let recaptchaResolve: ((value: void | PromiseLike<void>) => void) | null = null;
let recaptchaReject: ((reason?: any) => void) | null = null;
let recaptchaLoadingPromise: Promise<void> | null = null;

(window as any).vueRecaptchaApiLoaded = () => {
    if (recaptchaResolve) {
        recaptchaResolve();
        recaptchaLoadingPromise = null;
        recaptchaResolve = null;
        recaptchaReject = null;
    }
};

export const loadRecaptchaScript = (): Promise<void> => {
    const lang = i18n.global.locale.value;
    const existingScript = document.getElementById('recaptcha-script') as HTMLScriptElement | null;

    if (existingScript) {
        const loadedLang = new URL(existingScript.src).searchParams.get('hl');
        if (loadedLang !== lang) {
            existingScript.remove();
            recaptchaLoadingPromise = null;
            if (window.grecaptcha) delete (window as any).grecaptcha;
            console.log(`Removed reCAPTCHA script for language: ${loadedLang}. Requesting for ${lang}.`);
        }
    }

    // 이미 로드 요청이 진행 중이면, 해당 Promise를 반환
    if (recaptchaLoadingPromise) {
        return recaptchaLoadingPromise;
    }

    // 새로운 Promise 생성
    recaptchaLoadingPromise = new Promise((resolve, reject) => {
        recaptchaResolve = resolve;
        recaptchaReject = reject;

        // 스크립트가 이미 DOM에 있지만 Promise만 없는 경우 (매우 드문 케이스 방지)
        if (document.getElementById('recaptcha-script')) {
            console.warn('reCAPTCHA script tag found without a loading promise. Waiting for onload callback.');
            return;
        }

        const script = document.createElement('script');
        script.id = 'recaptcha-script';
        script.src = `https://www.google.com/recaptcha/api.js?onload=vueRecaptchaApiLoaded&render=explicit&hl=${lang}`;
        script.async = true;
        script.defer = true;

        script.onerror = (error) => {
            console.error('Failed to load reCAPTCHA script:', error);
            if (document.getElementById('recaptcha-script')) {
                document.getElementById('recaptcha-script')?.remove();
            }
            // 실패 시 Promise와 함수들을 모두 초기화
            recaptchaLoadingPromise = null;
            recaptchaResolve = null;
            if (recaptchaReject) {
                recaptchaReject('Failed to load reCAPTCHA script.');
                recaptchaReject = null;
            }
        };

        document.head.appendChild(script);
    });

    return recaptchaLoadingPromise;
};