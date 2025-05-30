package com.koo.bonscore.common.masking.support;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.koo.bonscore.common.masking.annotation.Mask;
import com.koo.bonscore.core.config.web.WebConfig;

import java.io.IOException;

/**
 * <pre>
 * MaskingSerializer.java
 *
 * 설명 : 민감한 개인정보(예: 이름, 이메일 등)를 JSON 직렬화 시 마스킹 처리하기 위한 커스텀 Jackson Serializer.
 *        필드에 {@link com.fasterxml.jackson.databind.annotation.JsonSerialize}와 {@link Mask} 어노테이션을 함께 사용하여,
 *        해당 필드의 값을 특정 마스킹 형식으로 변환함.
 *
 * 사용 예:
 *
 * {@code
 *     @Mask(type = MaskingType.NAME)
 *     @JsonSerialize(using = MaskingSerializer.class)
 *     private String userName;
 * }
 *
 * 이 경우 JSON 응답 시 userName 값이 마스킹되어 출력됨.
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-28
 * @see Mask
 * @see MaskingType
 * @see com.fasterxml.jackson.databind.JsonSerializer
 */
public class MaskingSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final MaskingType type;

    // 기본 생성자 (ContextualSerializer가 사용)
    public MaskingSerializer() {
        this.type = null;
    }

    public MaskingSerializer(MaskingType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (!MaskingContext.isMaskingEnabled() || type == null) {
            gen.writeString(value);
            return;
        }

        String masked = MaskingUtil.mask(value, type);
        gen.writeString(masked);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property != null) {
            Mask mask = property.getAnnotation(Mask.class);
            if (mask == null) {
                mask = property.getContextAnnotation(Mask.class);
            }
            if (mask != null) {
                return new MaskingSerializer(mask.type());
            }
        }
        return new MaskingSerializer(); // 마스킹 설정 없음
    }
}
