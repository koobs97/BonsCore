declare module '*.vue' {
    import type { DefineComponent } from 'vue';
    const component: DefineComponent<{}, {}, any>;
    export default component;
}
interface Window {
    grecaptcha: {
        render: (
            container: string | HTMLElement,
            parameters: {
                sitekey: string;
                callback: (token: string) => void;
                'expired-callback'?: () => void;
            }
        ) => number;
        reset: (widgetId?: number) => void;
    };
}