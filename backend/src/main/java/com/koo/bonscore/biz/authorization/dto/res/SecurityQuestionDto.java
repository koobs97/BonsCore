package com.koo.bonscore.biz.authorization.dto.res;

import lombok.*;

import java.time.LocalDateTime;

/**
 * <pre>
 * SecurityQuestionDto.java
 * 설명 : 보안질문 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-09-01
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityQuestionDto {
    private String questionCode;
    private String questionText;
}
