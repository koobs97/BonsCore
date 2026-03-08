package com.koo.bonscore.biz.messages.service;

import com.koo.bonscore.biz.messages.dto.req.MessageDeleteRequestDto;
import com.koo.bonscore.biz.messages.dto.req.MessageManageRequestDto;
import com.koo.bonscore.biz.messages.dto.req.MessageSaveRequestDto;
import com.koo.bonscore.biz.messages.dto.res.MessageManageResponseDto;
import com.koo.bonscore.biz.messages.entity.MessageResource;
import com.koo.bonscore.biz.messages.repository.MessageResourceRepository;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <pre>
 * MessageManageService.java
 * 설명 : 다국어 메시지 리소스 관리 서비스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-12-11
 */
@Service
@RequiredArgsConstructor
public class MessageManageService {

    private final MessageResourceRepository messageRepository;

    /**
     * 메시지 조회
     *
     * @param requestDto 조회 조건 (locale: 언어코드, message: 검색 키워드)
     * @return 조회된 메시지 리소스 목록 (List<MessageManageResponseDto>)
     */
    @Transactional(readOnly = true)
    public List<MessageManageResponseDto> getMessages(MessageManageRequestDto requestDto) {

        String lang = requestDto.getLocale();       // "ALL", "ko", "en"
        String keyword = requestDto.getMessage();   // 검색어 (code나 message에 포함된 텍스트)

        // 2. 변경된 리포지토리 메소드 호출
        // null 처리를 위해 keyword가 null이면 "" 빈문자열로 처리해도 되지만, 쿼리에서 IS NULL 체크를 하므로 그대로 넘겨도 됨
        List<MessageResource> messageResources = messageRepository.searchByLocaleAndKeyword(lang, keyword);

        return messageResources.stream()
                .map(resource -> MessageManageResponseDto.builder()
                        .id(resource.getId())
                        .code(resource.getCode())
                        .locale(resource.getLocale())
                        .message(resource.getMessage())
                        .createdAt(resource.getCreatedAt())
                        .updatedAt(resource.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 다국어 메시지 일괄 저장 (등록/수정)
     * <pre>
     * 전달받은 메시지 리스트를 순회하며 저장한다.
     * ID가 null이면 신규 등록(Insert), 존재하면 변경 감지(Dirty Checking)를 통해 수정(Update)한다.
     * 데이터 변경 시 'i18nMessages' 캐시를 전체 초기화하여 반영한다.
     * </pre>
     * @param requestDtos 저장할 메시지 리스트 (KO, EN 포함)
     */
    @Transactional
    @CacheEvict(value = "i18nMessages", allEntries = true) // 저장 후 캐시 전체 삭제
    public void saveMessages(List<MessageSaveRequestDto> requestDtos) {

        for (MessageSaveRequestDto dto : requestDtos) {
            if (dto.getId() == null) {
                // 1. 신규 등록 (ID가 없는 경우)
                // 중복 방지를 위해 코드+로케일로 기존 데이터가 있는지 체크하는 것이 안전함 (선택 사항)
                // messageRepository.findByCodeAndLocale(dto.getCode(), dto.getLocale()).ifPresent(...);

                MessageResource newMessage = MessageResource.builder()
                        .code(dto.getCode())
                        .locale(dto.getLocale())
                        .message(dto.getMessage())
                        .build();
                messageRepository.save(newMessage);

            } else {
                // 2. 수정 (ID가 있는 경우)
                MessageResource existingMessage = messageRepository.findById(dto.getId())
                        .orElseThrow(() -> new BsCoreException(ErrorCode.MESSAGE_NOT_FOUND));

                // Dirty Checking을 통한 업데이트
                existingMessage.updateMessage(dto.getMessage());
            }
        }
    }

    /**
     * 메시지 그룹 삭제
     * <pre>
     * 특정 Code Key에 해당하는 모든 언어의 메시지 데이터를 삭제한다.
     * 삭제 전 데이터 존재 여부를 확인하며, 데이터 변경 시 캐시를 초기화한다.
     * </pre>
     * @param requestDto 삭제할 메시지의 Code를 담은 요청 객체
     */
    @Transactional
    @CacheEvict(value = "i18nMessages", allEntries = true) // 캐시 초기화
    public void deleteMessageByCode(MessageDeleteRequestDto requestDto) {
        String code = requestDto.getCode();

        // 1. 존재 여부 확인
        if (!messageRepository.existsByCode(code)) {
            // 404 Not Found 예외 발생
            throw new BsCoreException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        // 2. 삭제 수행 (해당 code를 가진 모든 로케일 데이터 삭제)
        messageRepository.deleteByCode(code);
    }
}
