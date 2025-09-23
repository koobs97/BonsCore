package com.koo.bonscore.biz.store.controller;

import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import com.koo.bonscore.biz.store.service.GourmetRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gourmet-records")
@RequiredArgsConstructor
public class GourmetRecordController {

    private final GourmetRecordService gourmetRecordService;

    @PostMapping
    public ResponseEntity<Void> createGourmetRecord(@RequestBody GourmetRecordCreateRequest request,
                                                    @AuthenticationPrincipal String userId) { // 예: Spring Security 사용 시
        // TODO: 실제 프로젝트에 맞게 사용자 ID를 가져오는 로직 구현 필요
        // 예시로 "testuser" 하드코딩
        request.setUserId("testuser");

        gourmetRecordService.createGourmetRecord(request);
        return ResponseEntity.ok().build();
    }
}
