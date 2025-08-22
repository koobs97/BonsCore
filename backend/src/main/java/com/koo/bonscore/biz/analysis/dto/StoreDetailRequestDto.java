package com.koo.bonscore.biz.analysis.dto;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class StoreDetailRequestDto {
    private String name;
    private String simpleAddress;
    private String detailAddress;

    private String selectedTime;
}
