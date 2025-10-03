package com.koo.bonscore.biz.authorization.dto.req;

import lombok.*;

/**
 * <pre>
 * UserReqDto.java
 * 설명 : 관리자의 사용자 조회 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-10-03
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReqDto {
    private String userId;
    private String userName;
    private String email;
    private String accountLocked;
    private String withdrawn;
}
