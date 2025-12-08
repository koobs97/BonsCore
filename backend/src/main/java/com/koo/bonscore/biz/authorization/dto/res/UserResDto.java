package com.koo.bonscore.biz.authorization.dto.res;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.koo.bonscore.common.masking.annotation.Mask;
import com.koo.bonscore.common.masking.jackson.MaskingSerializer;
import com.koo.bonscore.common.masking.type.MaskingType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * <pre>
 * UserResDto.java
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
public class UserResDto {
    private String userId;
    @Mask(type = MaskingType.NAME)
    @JsonSerialize(using = MaskingSerializer.class)
    private String userName;
    @Mask(type = MaskingType.EMAIL)
    @JsonSerialize(using = MaskingSerializer.class)
    private String email;
    @Mask(type = MaskingType.PHONE)
    @JsonSerialize(using = MaskingSerializer.class)
    private String phoneNumber;
    private String createdAt;
    private String accountLocked;
    private String withdrawn;
    private LocalDateTime lastLoginAt;
}
