package com.koo.bonscore.common.message.service;

import com.koo.bonscore.common.message.dto.req.MessageRequestDto;
import com.koo.bonscore.common.message.dto.res.MessageResponseDto;
import com.koo.bonscore.common.message.entity.MessageResource;
import com.koo.bonscore.common.message.repository.MessageResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageResourceRepository messageRepository;

    /**
     * 특정 언어의 메시지를 조회하여 Nested Map 구조로 반환
     * @param lang 언어 코드 (ko, en)
     * @return Vue i18n용 JSON 구조
     */
    @Cacheable(value = "i18nMessages", key = "#lang")
    @Transactional(readOnly = true)
    public Map<String, Object> getMessagesByLocale(String lang) {
        List<MessageResource> resources = messageRepository.findAllByLocale(lang);
        Map<String, Object> resultMap = new HashMap<>();

        for (MessageResource resource : resources) {
            // 점(.)을 기준으로 키를 분리하여 계층 구조 생성
            // 예: "login.title" -> ["login", "title"]
            String[] keys = resource.getCode().split("\\.");

            Map<String, Object> currentMap = resultMap;

            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];

                // 마지막 키인 경우 메시지 값 할당
                if (i == keys.length - 1) {
                    currentMap.put(key, resource.getMessage());
                } else {
                    // 중간 키인 경우 Map을 생성하거나 가져옴
                    // "login" 키가 없으면 새로운 Map 생성, 있으면 가져오기
                    currentMap = (Map<String, Object>) currentMap.computeIfAbsent(key, k -> new HashMap<>());
                }
            }
        }

        return resultMap;
    }

    @Transactional(readOnly = true)
    public List<MessageResponseDto> getMessages(MessageRequestDto requestDto) {

        String lang = requestDto.getLocale();       // "ALL", "ko", "en"
        String keyword = requestDto.getMessage();   // 검색어 (code나 message에 포함된 텍스트)

        // 2. 변경된 리포지토리 메소드 호출
        // null 처리를 위해 keyword가 null이면 "" 빈문자열로 처리해도 되지만, 쿼리에서 IS NULL 체크를 하므로 그대로 넘겨도 됨
        List<MessageResource> messageResources = messageRepository.searchByLocaleAndKeyword(lang, keyword);

        return messageResources.stream()
                .map(resource -> MessageResponseDto.builder()
                        .id(resource.getId())
                        .code(resource.getCode())
                        .locale(resource.getLocale())
                        .message(resource.getMessage())
                        .createdAt(resource.getCreatedAt())
                        .updatedAt(resource.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    // 'i18nMessages' 캐시의 모든 항목(allEntries=true)을 삭제하여
    // 다음 조회 때 DB에서 다시 읽어오도록 강제함
    @CacheEvict(value = "i18nMessages", allEntries = true)
    public void saveMessage(MessageResource messageResource) {
        messageRepository.save(messageResource);
    }
}
