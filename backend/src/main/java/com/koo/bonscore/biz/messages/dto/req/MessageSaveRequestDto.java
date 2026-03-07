package com.koo.bonscore.biz.messages.dto.req;

import lombok.*;

/**
 * <pre>
 * MessageSaveRequestDto.java
 * 설명 : 메시지 저장 dto
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
public class MessageSaveRequestDto {
    private Long id;          // 수정 시 필요 (등록 시 null)
    private String code;      // 메시지 코드
    private String locale;    // 'ko' or 'en'
    private String message;   // 메시지 내용
}
