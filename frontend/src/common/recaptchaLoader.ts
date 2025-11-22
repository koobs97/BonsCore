import i18n from '@/i18n';

let recaptchaResolve: ((value: void | PromiseLike<void>) => void) | null = null;
let recaptchaReject: ((reason?: any) => void) | null = null;
let recaptchaLoadingPromise: Promise<void> | null = null;

// window 전역 객체에 콜백 함수 할당
(window as any).vueRecaptchaApiLoaded = () => {
    if (recaptchaResolve) {
        recaptchaResolve();
        // 로딩이 완료되면 Promise 변수는 초기화하되,
        // 스크립트 태그와 window.grecaptcha 객체는 남아있음.
        recaptchaLoadingPromise = null;
        recaptchaResolve = null;
        recaptchaReject = null;
    }
};

export const loadRecaptchaScript = (): Promise<void> => {
    const lang = i18n.global.locale.value;
    const existingScript = document.getElementById('recaptcha-script') as HTMLScriptElement | null;
    const isRecaptchaLoaded = (window as any).grecaptcha && (window as any).grecaptcha.render;

    // 1. [핵심 수정] 이미 스크립트가 있고 + 언어도 같고 + 객체도 로드되어 있다면?
    // 기다릴 필요 없이 즉시 완료 처리 (불필요한 중복 로딩 및 경고 방지)
    if (existingScript && isRecaptchaLoaded) {
        const loadedLang = new URL(existingScript.src).searchParams.get('hl');
        if (loadedLang === lang) {
            return Promise.resolve();
        }
    }

    // 2. 스크립트가 있지만 언어가 다른 경우 -> 삭제 후 재로딩 진행
    if (existingScript) {
        const loadedLang = new URL(existingScript.src).searchParams.get('hl');
        if (loadedLang !== lang) {
            existingScript.remove();
            recaptchaLoadingPromise = null;
            if ((window as any).grecaptcha) delete (window as any).grecaptcha;
            console.log(`Removed reCAPTCHA script for language: ${loadedLang}. Requesting for ${lang}.`);
        } else if (!isRecaptchaLoaded && !recaptchaLoadingPromise) {
            // 3. 태그는 있는데 객체가 없고, 로딩 중인 Promise도 없다? (매우 드문 에러 상황)
            // 기존 태그를 지우고 새로 받는 것이 안전함.
            existingScript.remove();
        }
    }

    // 4. 이미 로딩이 진행 중이라면 해당 Promise 반환
    if (recaptchaLoadingPromise) {
        return recaptchaLoadingPromise;
    }

    // 5. 새로운 로딩 시작
    recaptchaLoadingPromise = new Promise((resolve, reject) => {
        recaptchaResolve = resolve;
        recaptchaReject = reject;

        const script = document.createElement('script');
        script.id = 'recaptcha-script';
        script.src = `https://www.google.com/recaptcha/api.js?onload=vueRecaptchaApiLoaded&render=explicit&hl=${lang}`;
        script.async = true;
        script.defer = true;

        script.onerror = (error) => {
            console.error('Failed to load reCAPTCHA script:', error);
            const badScript = document.getElementById('recaptcha-script');
            if (badScript) badScript.remove();

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