package com.koo.bonscore.common.paging.support;

import com.koo.bonscore.common.paging.dto.res.PageData;
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

    /**
     * 페이징 처리된 결과 객체를 생성
     *
     * @param <T> 결과 리스트의 제네릭 타입
     * @param list 현재 페이지에 해당하는 데이터 리스트
     * @param vo 페이징 요청 정보를 담고 있는 객체
     * @return 페이징 정보를 포함한 {@link PageResult} 객체
     *
     */
    public static <T> PageResult<T> create(List<T> list, PagingVo vo) {
        int totalCount = PageContext.getTotalCount() != null ? PageContext.getTotalCount() : 0;
        int pageNum = vo.getPage() != null ? vo.getPage().getPageNum() : 1;
        int pageSize = vo.getPage() != null ? vo.getPage().getPageSize() : 20;

        PageData pageData = new PageData(pageNum, pageSize, totalCount);

        return new PageResult<>(pageData, list);
    }
}
