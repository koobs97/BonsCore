package com.koo.bonscore.biz.authorization.dto.res;

import lombok.*;

import java.util.List;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponseDto {
    private List<LogResDto> activityTypeList;
    private List<LogResDto> activityResultList;
}
