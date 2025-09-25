package com.koo.bonscore.biz.store.controller;

import com.koo.bonscore.biz.store.dto.req.GourmetRecordCreateRequest;
import com.koo.bonscore.biz.store.dto.res.GourmetRecordDto;
import com.koo.bonscore.biz.store.service.GourmetRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gourmet-records")
@RequiredArgsConstructor
public class GourmetRecordController {

    private final GourmetRecordService gourmetRecordService;

    @PostMapping("/write")
    public ResponseEntity<Void> createGourmetRecord(@RequestBody GourmetRecordCreateRequest request) {
        gourmetRecordService.createGourmetRecord(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read")
    public ResponseEntity<List<GourmetRecordDto>> getGourmetRecords(@RequestBody GourmetRecordCreateRequest request) {
        List<GourmetRecordDto> records = gourmetRecordService.getGourmetRecords(request);
        return ResponseEntity.ok(records);
    }
}
