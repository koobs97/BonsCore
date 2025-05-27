package com.koo.bonscore.sample.vo;

import com.koo.bonscore.common.page.Page;
import lombok.Data;

@Data
public class SampleVo {

    private Page page;

    private String userId;
    private String userName;
    private String email;
}
