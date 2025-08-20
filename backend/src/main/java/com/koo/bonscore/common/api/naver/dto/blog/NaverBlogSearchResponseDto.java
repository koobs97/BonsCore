package com.koo.bonscore.common.api.naver.dto.blog;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NaverBlogSearchResponseDto {
    private String lastBuildDate;
    private int total;
    private int start;
    private int display;
    private List<NaverBlogItemDto> items;
}
