import { ref, watch, onMounted, onUnmounted } from 'vue';

type Theme = 'light' | 'dark' | 'system';

export function useTheme() {
    // 1. localStorage에서 저장된 테마를 가져오거나, 없으면 'system'을 기본값으로 설정
    const theme = ref<Theme>((localStorage.getItem('theme') as Theme) || 'system');

    // 2. 테마를 실제 DOM에 적용하는 함수
    const applyTheme = (newTheme: Theme) => {
        const root = document.documentElement;
        if (newTheme === 'dark' || (newTheme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
            root.classList.add('dark');
        } else {
            root.classList.remove('dark');
        }
    };

    // 3. 시스템 테마 변경을 감지하기 위한 MediaQueryList
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

    // 4. 시스템 테마 변경 감지 이벤트 핸들러
    const handleSystemThemeChange = () => {
        if (theme.value === 'system') {
            applyTheme('system');
        }
    };

    // 5. theme ref의 값이 변경될 때마다 localStorage에 저장하고 DOM에 적용
    watch(theme, (newTheme) => {
        localStorage.setItem('theme', newTheme);
        applyTheme(newTheme);
    }, { immediate: true }); // immediate: true로 컴포넌트 마운트 시 즉시 실행

    // 6. 컴포넌트가 마운트/언마운트될 때 이벤트 리스너 등록/해제
    onMounted(() => {
        mediaQuery.addEventListener('change', handleSystemThemeChange);
    });

    onUnmounted(() => {
        mediaQuery.removeEventListener('change', handleSystemThemeChange);
    });

    // 7. 외부에서 사용할 수 있도록 theme ref와 theme를 변경하는 함수를 반환
    const setTheme = (newTheme: Theme) => {
        theme.value = newTheme;
    };

    return {
        theme,
        setTheme,
    };
}