package com.koo.bonscore.biz.auth.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 * LoginCheckDto.java
 * 설명 : 로그인 체크에 사용되는 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-10-06
 */
@Getter
@Setter
public class LoginCheckDto {
    private String passwordHash;
    private String accountLocked;
}
