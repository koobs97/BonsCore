package com.koo.bonscore.biz.auth.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.koo.bonscore.common.masking.annotation.Mask;
import com.koo.bonscore.common.masking.jackson.MaskingSerializer;
import com.koo.bonscore.common.masking.type.MaskingType;
import lombok.*;

import java.time.LocalDateTime;

/**
 * <pre>
 * UserInfoSearchDto.java
 * 설명 : 인증 서비스에서 주로 사용되는 dto 클래스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-07-24
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoSearchDto {
    @Mask(type = MaskingType.ID)
    @JsonSerialize(using = MaskingSerializer.class)
    private String userId;
    private String nonMaskedId;
    private String userName;
    private String email;
    private String code;
    private String type;
    private String token;
    private String password;
    private String passwordUpdated;
    private String passwordHintAnswer;
    private LocalDateTime updatedAt;
}
