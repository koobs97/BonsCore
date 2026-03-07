package com.koo.bonscore.biz.messages.dto.req;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <pre>
 * MessageDeleteRequestDto.java
 * 설명 : 삭제할 메시지 그룹 코드 dto (예: login.title)
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-12-11
 */
@Getter
@Setter
@NoArgsConstructor
public class MessageDeleteRequestDto {
    private String code;
}
