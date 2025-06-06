package com.koo.bonscore.sample.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.koo.bonscore.common.masking.annotation.Mask;
import com.koo.bonscore.common.masking.jackson.MaskingSerializer;
import com.koo.bonscore.common.masking.type.MaskingType;
import lombok.Data;

import java.util.List;

/**
 * <pre>
 * MaskingVo.java
 * 설명 : 마스킹처리 vo
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-25
 */

@Data
public class MaskingVo {

    private String userId;
    private Boolean maskingEnabled;

    @Mask(type = MaskingType.NAME)
    @JsonSerialize(using = MaskingSerializer.class)
    private String userName;

    @Mask(type = MaskingType.EMAIL)
    @JsonSerialize(using = MaskingSerializer.class)
    private String email;

    private List<MaskingVo> list;
}
