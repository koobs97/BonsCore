package com.koo.bonscore.biz.authorization.dto.res;

import lombok.*;

import java.util.List;

/**
 * <pre>
 * ActivityResponseDto.java
 * 설명 : 활동로그 조회 화면의 검색조건에 사용될 코드결과 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-13
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponseDto {
    private List<LogResDto> activityTypeList;
    private List<LogResDto> activityResultList;
}
