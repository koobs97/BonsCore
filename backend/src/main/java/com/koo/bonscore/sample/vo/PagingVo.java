package com.koo.bonscore.sample.vo;

import com.koo.bonscore.common.paging.dto.req.Page;
import lombok.Data;

/**
 * <pre>
 * PagingVo.java
 * 설명 : 페이징처리 샘플 Request vo
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-25
 */
@Data
public class PagingVo {

    private Page page;

    private String userId;
    private String userName;
    private String email;
}
