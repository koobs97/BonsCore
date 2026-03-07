package com.koo.bonscore.biz.messages.dto.res;

import lombok.*;

import java.time.LocalDateTime;

/**
 * <pre>
 * MessageManageResponseDto.java
 * 설명 : 메시지 조회 결과 반환 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-12-11
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageManageResponseDto {
    private Long id;
    private String code;
    private String locale;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
