package com.koo.bonscore.biz.store.controller;

import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import com.koo.bonscore.biz.store.dto.res.GourmetRecordDto;
import com.koo.bonscore.biz.store.service.GourmetRecordService;
import com.koo.bonscore.common.file.service.FileStorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Gourmet Records", description = "맛집 기록 API")
@Slf4j
@RestController
@RequestMapping("/api/gourmet-records")
@RequiredArgsConstructor
public class GourmetRecordController {

    private final GourmetRecordService gourmetRecordService;
    private final FileStorageService fileStorageService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<Void> createGourmetRecord(@RequestBody GourmetRecordCreateRequest request) {
        gourmetRecordService.saveGourmetRecord(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<List<GourmetRecordDto>> getGourmetRecords(
            @RequestHeader(value = "Accept-Language", defaultValue = "ko") String lang,
            @AuthenticationPrincipal UserDetails userDetail) {
        String userId = userDetail.getUsername();
        List<GourmetRecordDto> records = gourmetRecordService.getGourmetRecords(userId, lang);
        return ResponseEntity.ok(records);
    }

    @DeleteMapping("/temp-files")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public void deleteOldTempFiles(@AuthenticationPrincipal UserDetails userDetail) {
        String userId = userDetail.getUsername();
        fileStorageService.cleanupUserTempFiles(userId);
    }
}
