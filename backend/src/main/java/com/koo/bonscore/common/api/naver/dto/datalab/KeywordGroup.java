package com.koo.bonscore.common.api.naver.dto.datalab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class KeywordGroup {
    private String groupName;
    private List<String> keywords;
}