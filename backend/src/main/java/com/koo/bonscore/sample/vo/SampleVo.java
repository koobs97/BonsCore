package com.koo.bonscore.sample.vo;

import com.koo.bonscore.common.paging.Page;
import lombok.Data;

@Data
public class SampleVo {

    private Page page;

    private String userId;
    private String userName;
    private String email;
}
