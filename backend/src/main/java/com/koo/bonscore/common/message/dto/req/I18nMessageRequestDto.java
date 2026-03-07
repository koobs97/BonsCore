package com.koo.bonscore.common.message.dto.req;

import lombok.*;

/**
 * <pre>
 * MessageRequestDto.java
 * 설명 : 메시지 조회 조건 dto
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
public class I18nMessageRequestDto {
    private String locale;
    private String message;
}
