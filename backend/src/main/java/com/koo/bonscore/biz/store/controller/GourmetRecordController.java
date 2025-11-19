package com.koo.bonscore.biz.store.controller;

import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import com.koo.bonscore.biz.store.dto.res.GourmetRecordDto;
import com.koo.bonscore.biz.store.service.GourmetRecordService;
import com.koo.bonscore.common.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class GourmetRecordController {

    private final GourmetRecordService gourmetRecordService;
    private final FileStorageService fileStorageService;

    @PostMapping("/write")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<Void> createGourmetRecord(@RequestBody GourmetRecordCreateRequest request) {
        gourmetRecordService.saveGourmetRecord(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<List<GourmetRecordDto>> getGourmetRecords(
            @RequestHeader(value = "Accept-Language", defaultValue = "ko") String lang,
            @AuthenticationPrincipal UserDetails userDetail) {
        String userId = userDetail.getUsername();
        List<GourmetRecordDto> records = gourmetRecordService.getGourmetRecords(userId, lang);
        return ResponseEntity.ok(records);
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public void deleteOldTempFiles(@AuthenticationPrincipal UserDetails userDetail) {
        String userId = userDetail.getUsername();
        fileStorageService.cleanupUserTempFiles(userId);
    }
}
