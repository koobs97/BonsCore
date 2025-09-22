package com.koo.bonscore.biz.store.mapper;

import com.koo.bonscore.biz.store.dto.res.StoreDetailResponseDto;
import com.koo.bonscore.biz.store.dto.res.StoreSimpleResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

// com.yourcompany.bonscore.domain.store.StoreMapper.java
@Mapper
public interface StoreMapper {

    /**
     * 새로운 맛집 정보를 STORE 테이블에 저장합니다.
     * @param store 저장할 맛집 엔티티 객체 (ID는 비어있음)
     */
    // void insertStore(Store store);

    /**
     * 맛집에 첨부된 이미지 정보들을 STORE_IMAGE 테이블에 저장합니다. (배치 처리)
     * @param images 저장할 이미지 엔티티 리스트
     */
    // void insertStoreImages(@Param("images") List<StoreImage> images);

    /**
     * 모든 맛집의 간단한 정보를 최신 방문일 순으로 조회합니다.
     * @return StoreSimpleResponseDto 리스트
     */
    List<StoreSimpleResponseDto> findAllStores();

    /**
     * 특정 ID를 가진 맛집의 상세 정보를 이미지와 함께 조회합니다.
     * @param id 조회할 맛집의 ID
     * @return StoreDetailResponseDto
     */
    StoreDetailResponseDto findStoreById(Long id);
}
