package com.koo.bonscore.common.message.dto.res;

import lombok.*;

import java.time.LocalDateTime;

/**
 * <pre>
 * MessageResponseDto.java
 * 설명 : 메시지 조회 결과 반환 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-12-09
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponseDto {
    private Long id;
    private String code;
    private String locale;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
