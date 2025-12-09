package com.koo.bonscore.common.message.controller;

import com.koo.bonscore.common.message.dto.req.MessageRequestDto;
import com.koo.bonscore.common.message.dto.res.MessageResponseDto;
import com.koo.bonscore.common.message.service.MessageService;
import com.koo.bonscore.log.annotaion.UserActivityLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/common")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 다국어 메시지 조회 API
     * GET /api/common/messages?lang=ko
     */
    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> getMessages(@RequestParam(defaultValue = "ko") String lang) {
        Map<String, Object> messages = messageService.getMessagesByLocale(lang);
        return ResponseEntity.ok(messages);
    }

    @UserActivityLog(activityType = "GET_MESSAGES")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PostMapping("/messageManage")
    public List<MessageResponseDto> getMessagesManage(@RequestBody MessageRequestDto requestDto) {
        return messageService.getMessages(requestDto);
    }
}
