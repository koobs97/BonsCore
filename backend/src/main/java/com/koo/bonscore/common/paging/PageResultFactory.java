package com.koo.bonscore.common.paging;

import com.koo.bonscore.common.paging.dto.res.PageResult;
import com.koo.bonscore.sample.vo.PagingVo;
import java.util.List;

/**
 * <pre>
 * PageResultFactory.java
 * 설명 : 서비스에서 PageResult 형식으로 변환하기 위한 공통 util
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-05-28
 */
public class PageResultFactory {

    public static <T> PageResult<T> create(List<T> list, PagingVo vo) {
        int totalCount = PageContext.getTotalCount() != null ? PageContext.getTotalCount() : 0;
        int pageNum = vo.getPage() != null ? vo.getPage().getPageNum() : 1;

        return new PageResult<>(list, totalCount, pageNum, totalCount); // totalPages는 필요에 따라 계산 가능
    }
}
