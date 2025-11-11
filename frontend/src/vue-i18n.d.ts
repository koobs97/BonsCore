import 'vue'; // Vue 모듈을 임포트하여 Vue의 타입 시스템을 확장합니다.
import { Composer, I18n } from 'vue-i18n'; // vue-i18n의 타입들을 임포트합니다.

declare module '@vue/runtime-core' {
    // Vue의 ComponentCustomProperties 인터페이스를 확장합니다.
    // 여기에 플러그인이 주입하는 전역 속성들을 정의합니다.
    interface ComponentCustomProperties {
        $i18n: I18n<any, any, any, any>; // i18n 인스턴스 자체
        $t: Composer['t'];               // 번역 함수 (메시지 key를 받아서 번역된 문자열 반환)
        $tc: Composer['tc'];             // 레거시 복수형 번역 함수 (Vue I18n v8까지 사용되던 기능)
        $te: Composer['te'];             // 번역 key 존재 여부 확인 함수
        $d: Composer['d'];               // 날짜 포맷 함수
        $n: Composer['n'];               // 숫자 포맷 함수
    }
}