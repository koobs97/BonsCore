package com.koo.bonscore.core.config.web.security.util;

import io.micrometer.common.util.StringUtils;

import java.util.regex.Pattern;

/**
 * <pre>
 * CommandInjectionPreventUtil.java
 * 설명 : 운영체제 명령어 삽입 방지를 위한 유틸 서비스. 안전한 명령어 실행 기능 포함
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-06-03
 */
public class CommandInjectionPreventUtil {

    // 허용하는 문자 패턴: 영문 대소문자, 숫자, 하이픈, 언더스코어, 점, 슬래시
    private static final Pattern SAFE_INPUT_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_.\\/]+$");

    /**
     * 입력값이 안전한지 검증하는 메서드.
     * 허용된 문자(화이트리스트)만 포함되어야 true 반환.
     *
     * @param input 사용자 입력값
     * @return 안전하면 true, 아니면 false
     */
    public static boolean isSafeInput(String input) {
        if (StringUtils.isEmpty(input)) {
            return false;
        }
        return SAFE_INPUT_PATTERN.matcher(input).matches();
    }

    /**
     * 안전한 입력값일 경우 명령어 실행용 인자 배열 생성.
     * 화이트리스트 검증 후 인자 배열 생성.
     *
     * @param command 실행할 명령어
     * @param userInput 사용자 입력값
     * @return 명령어 배열 (command, userInput)
     * @throws IllegalArgumentException 입력값 검증 실패 시 예외 발생
     */
    public static String[] buildSafeCommand(String command, String userInput) {
        if (!isSafeInput(userInput)) {
            throw new IllegalArgumentException("Invalid input detected: " + userInput);
        }
        return new String[] {command, userInput};
    }

    /**
     * 명령어 실행 메서드 (예시)
     *
     * @param command 실행할 명령어
     * @param userInput 사용자 입력값
     * @return 실행 결과 출력 문자열 (간단 예시)
     * @throws Exception 실행 오류 시
     */
    public static String executeSafeCommand(String command, String userInput) throws Exception {
        String[] cmdArray = buildSafeCommand(command, userInput);

        Process process = Runtime.getRuntime().exec(cmdArray);

        // 프로세스 출력 결과를 간단히 읽어오는 코드는 필요에 따라 추가
        // 여기서는 생략

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command execution failed with code " + exitCode);
        }

        return "Command executed successfully";
    }
}
