package com.koo.bonscore.common.message.controller;

import com.koo.bonscore.common.message.service.I18nMessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Common", description = "공통 API (다국어 메시지 조회)")
@RestController
@RequestMapping("api/common")
@RequiredArgsConstructor
public class I18nMessageController {

    private final I18nMessageService messageService;

    /**
     * 다국어 메시지 조회 API
     * GET /api/common/messages?lang=ko
     */
    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> getMessages(@RequestParam(defaultValue = "ko") String lang) {
        Map<String, Object> messages = messageService.getMessagesByLocale(lang);
        return ResponseEntity.ok(messages);
    }
}
