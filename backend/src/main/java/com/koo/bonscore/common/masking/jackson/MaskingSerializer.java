package com.koo.bonscore.common.masking.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.koo.bonscore.common.masking.annotation.Mask;
import com.koo.bonscore.common.masking.context.MaskingContext;
import com.koo.bonscore.common.masking.type.MaskingType;
import com.koo.bonscore.common.masking.util.MaskingUtil;

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

    // 마스킹 타입 (이름, 이메일 등)
    private MaskingType type;

    /**
     * 기본 생성자 (ContextualSerializer가 인스턴스를 동적으로 만들기 위해 필요)
     */
    public MaskingSerializer() {
        // 기본 생성자 필요
    }

    /**
     * 마스킹 타입을 지정하는 생성자
     *
     * @param type 마스킹 처리 방식 (예: NAME, EMAIL)
     */
    public MaskingSerializer(MaskingType type) {
        this.type = type;
    }

    /**
     * 실제 JSON 직렬화 과정에서 호출되며, 주어진 문자열 값을 마스킹 처리하여 출력
     *
     * @param value        직렬화할 원본 값
     * @param gen          JSON 생성기
     * @param serializers  Serializer 제공자
     * @throws IOException JSON 출력 예외
     */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (!MaskingContext.isMaskingEnabled()) {
            gen.writeString(value); // 마스킹 OFF → 원본 그대로 출력
            return;
        }

        String masked = switch (type) {
            case ID -> MaskingUtil.maskId(value);
            case NAME -> MaskingUtil.maskName(value);
            case EMAIL -> MaskingUtil.maskEmail(value);
            default -> value;
        };
        gen.writeString(masked);
    }

    /**
     * 필드에 선언된 {@link Mask} 어노테이션을 분석하여 해당 타입으로 마스킹 처리 인스턴스를 생성
     *
     * @param prov     Serializer 제공자
     * @param property 현재 직렬화 중인 필드 정보
     * @return 새로운 MaskingSerializer 인스턴스
     * @throws JsonMappingException 매핑 예외
     */
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
        return this; // 어노테이션이 없으면 기존 인스턴스 그대로 사용
    }
}
