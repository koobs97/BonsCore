package com.koo.bonscore.common.file.controller;

import com.koo.bonscore.common.file.dto.FileResponse;
import com.koo.bonscore.common.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);
        String fileDownloadUri = fileStorageService.getFileDownloadUri(fileName);

        return ResponseEntity.ok(new FileResponse(fileName, fileDownloadUri, file.getContentType(), file.getSize()));
    }

    @PostMapping("/uploadMultiple")
    public ResponseEntity<List<FileResponse>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        List<FileResponse> responses = Arrays.stream(files)
                .map(this::uploadFile) // 위 uploadFile 메서드 재사용
                .map(ResponseEntity::getBody)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
