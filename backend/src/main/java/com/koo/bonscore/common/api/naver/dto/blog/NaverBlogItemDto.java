package com.koo.bonscore.common.api.naver.dto.blog;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NaverBlogItemDto {
    private String title;
    private String link;
    private String description;
    private String bloggername;
    private String bloggerlink;
    private String postdate;
}