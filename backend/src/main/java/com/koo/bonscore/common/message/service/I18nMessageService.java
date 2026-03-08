package com.koo.bonscore.common.message.service;

import com.koo.bonscore.biz.messages.entity.MessageResource;
import com.koo.bonscore.biz.messages.repository.MessageResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class I18nMessageService {

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
}
