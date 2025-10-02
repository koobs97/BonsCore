package com.koo.bonscore.biz.store.controller;

import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import com.koo.bonscore.biz.store.dto.res.GourmetRecordDto;
import com.koo.bonscore.biz.store.service.GourmetRecordService;
import com.koo.bonscore.common.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/gourmet-records")
@RequiredArgsConstructor
public class GourmetRecordController {

    private final GourmetRecordService gourmetRecordService;
    private final FileStorageService fileStorageService;

    @PostMapping("/write")
    public ResponseEntity<Void> createGourmetRecord(@RequestBody GourmetRecordCreateRequest request) {
        gourmetRecordService.saveGourmetRecord(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read")
    public ResponseEntity<List<GourmetRecordDto>> getGourmetRecords(
            @RequestBody GourmetRecordCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetail) {
        String userId = userDetail.getUsername();
        List<GourmetRecordDto> records = gourmetRecordService.getGourmetRecords(userId);
        return ResponseEntity.ok(records);
    }

    @PostMapping("/delete")
    public void deleteOldTempFiles(@AuthenticationPrincipal UserDetails userDetail) {
        String userId = userDetail.getUsername();
        fileStorageService.cleanupUserTempFiles(userId);
    }
}
