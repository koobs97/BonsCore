package com.koo.bonscore.common.masking.support;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.koo.bonscore.common.masking.annotation.Mask;

import java.io.IOException;

public class MaskingSerializer extends JsonSerializer<String> implements ContextualSerializer {
    private MaskingType type;

    public MaskingSerializer() {
        // 기본 생성자 필요
    }

    public MaskingSerializer(MaskingType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String masked = switch (type) {
            case NAME -> MaskingUtil.maskName(value);
            case EMAIL -> MaskingUtil.maskEmail(value);
            default -> value;
        };
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
        return this;
    }
}
