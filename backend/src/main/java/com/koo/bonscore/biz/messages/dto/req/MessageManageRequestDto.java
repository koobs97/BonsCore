package com.koo.bonscore.biz.messages.dto.req;

import lombok.*;

/**
 * <pre>
 * MessageManageRequestDto.java
 * 설명 : 메시지 조회 조건 dto
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
public class MessageManageRequestDto {
    private String locale;
    private String message;
}
