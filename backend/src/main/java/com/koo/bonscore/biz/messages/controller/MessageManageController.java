package com.koo.bonscore.biz.messages.controller;

import com.koo.bonscore.biz.messages.dto.req.MessageDeleteRequestDto;
import com.koo.bonscore.biz.messages.dto.req.MessageManageRequestDto;
import com.koo.bonscore.biz.messages.dto.req.MessageSaveRequestDto;
import com.koo.bonscore.biz.messages.dto.res.MessageManageResponseDto;
import com.koo.bonscore.biz.messages.service.MessageManageService;
import com.koo.bonscore.log.annotaion.UserActivityLog;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <pre>
 * MessageManageController.java
 * 설명 : 다국어 메시지 리소스 관리 컨트롤러
 *       관리자 권한으로 메시지의 조회, 등록, 수정, 삭제 기능을 수행
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-12-11
 */
@Tag(name = "Messages", description = "다국어 메시지 리소스 관리 API")
@RestController
@RequestMapping("api/messages")
@RequiredArgsConstructor
public class MessageManageController {

    private final MessageManageService messageManageService;

    /**
     * 메시지 관리 목록 조회
     *
     * @param requestDto 조회 조건 (locale: 언어코드, message: 검색 키워드)
     * @return 조회된 메시지 리소스 목록 (List<MessageManageResponseDto>)
     */
    @UserActivityLog(activityType = "GET_MESSAGES")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @GetMapping
    public List<MessageManageResponseDto> getMessagesManage(@ModelAttribute MessageManageRequestDto requestDto) {
        return messageManageService.getMessages(requestDto);
    }

    /**
     * 메시지 일괄 저장 (등록 및 수정)
     *
     * @param requestDtos 저장할 메시지 정보 리스트 (List<MessageSaveRequestDto>)
     */
    @UserActivityLog(activityType = "SAVE_MESSAGES")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PutMapping
    public void saveMessages(@RequestBody List<MessageSaveRequestDto> requestDtos) {
        messageManageService.saveMessages(requestDtos);
    }

    /**
     * 메시지 삭제
     *
     * @param code 삭제할 메시지의 Code
     */
    @UserActivityLog(activityType = "DELETE_MESSAGES")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @DeleteMapping("/{code}")
    public void deleteMessage(@PathVariable String code) {
        MessageDeleteRequestDto requestDto = new MessageDeleteRequestDto();
        requestDto.setCode(code);
        messageManageService.deleteMessageByCode(requestDto);
    }
}
