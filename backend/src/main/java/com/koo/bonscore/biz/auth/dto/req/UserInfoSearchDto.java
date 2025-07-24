package com.koo.bonscore.biz.auth.dto.req;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.koo.bonscore.common.masking.annotation.Mask;
import com.koo.bonscore.common.masking.jackson.MaskingSerializer;
import com.koo.bonscore.common.masking.type.MaskingType;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
public class UserInfoSearchDto {
    private String userName;
    private String email;

    @Mask(type = MaskingType.ID)
    @JsonSerialize(using = MaskingSerializer.class)
    private String userId;

    private String code;
}
