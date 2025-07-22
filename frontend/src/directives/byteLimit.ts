import type { Directive } from 'vue';

// --------------------------------------------------
// Helper Functions with Types
// --------------------------------------------------

/**
 * UTF-8 문자열의 바이트 길이를 계산합니다.
 * @param str - 입력 문자열
 * @returns 바이트 단위 길이
 */
const getByteLength = (str: string): number => {
    if (!str) return 0;
    return new TextEncoder().encode(str).length;
};

/**
 * 주어진 최대 바이트를 초과하지 않도록 문자열을 자릅니다.
 * @param str - 자를 문자열
 * @param maxBytes - 최대 허용 바이트
 * @returns 잘린 문자열
 */
const truncateByBytes = (str: string, maxBytes: number): string => {
    if (!str) return '';
    const encoder = new TextEncoder();
    let byteCount = 0;
    let truncatedStr = '';

    for (const char of str) {
        const charBytes = encoder.encode(char).length;
        if (byteCount + charBytes > maxBytes) {
            break;
        }
        byteCount += charBytes;
        truncatedStr += char;
    }
    return truncatedStr;
};

// --------------------------------------------------
// Directive Definition
// --------------------------------------------------

// 엘리먼트에 추가할 사용자 정의 속성을 위한 타입 정의
// 이렇게 하면 TypeScript가 el._byteLimitHandler의 존재를 인지할 수 있습니다.
interface ElWithDirective extends HTMLElement {
    _byteLimitHandler?: (event: Event) => void;
}

// Directive<El, V> 제네릭을 사용하여 타입 추론 강화
// El: 디렉티브가 바인딩된 엘리먼트의 타입
// V: 디렉티브에 전달된 값(binding.value)의 타입
export const byteLimit: Directive<ElWithDirective, number> = {
    mounted(el, binding) {
        const maxBytes = binding.value;
        if (!maxBytes || typeof maxBytes !== 'number') {
            console.warn('v-byte-limit directive requires a number value.');
            return;
        }

        const inputEl = el.querySelector('input') || el.querySelector('textarea');
        if (!inputEl) {
            console.warn('v-byte-limit could not find an input or textarea element.');
            return;
        }

        const handler = (event: Event) => {
            // EventTarget을 HTMLInputElement로 타입 단언
            const target = event.target as HTMLInputElement;
            const currentValue = target.value;
            const currentBytes = getByteLength(currentValue);

            if (currentBytes > maxBytes) {
                const truncatedValue = truncateByBytes(currentValue, maxBytes);

                // vue가 v-model 업데이트를 즉시 반영하지 않을 수 있으므로,
                // 다음 틱에서 DOM 업데이트가 완료된 후 값을 설정하는 것이 더 안정적일 수 있으나
                // 대부분의 경우 직접 할당이 잘 동작합니다.
                target.value = truncatedValue;

                // v-model이 업데이트되도록 'input' 이벤트를 수동으로 발생시킴
                target.dispatchEvent(new Event('input', { bubbles: true }));
            }
        };

        // 핸들러를 엘리먼트에 저장
        el._byteLimitHandler = handler;
        inputEl.addEventListener('input', handler);
    },

    unmounted(el) {
        const inputEl = el.querySelector('input') || el.querySelector('textarea');
        if (inputEl && el._byteLimitHandler) {
            inputEl.removeEventListener('input', el._byteLimitHandler);
            delete el._byteLimitHandler;
        }
    },
};