package com.koo.bonscore.common.file.controller;

import com.koo.bonscore.common.file.dto.FileResponse;
import com.koo.bonscore.common.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor // 다시 원래대로 복구
public class FileController {

    private final FileStorageService fileStorageService;
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        logger.info(">>>>> [FileController] /upload 진입...");

        try {
            // ▼▼▼▼▼▼▼ 핵심 수정 부분 ▼▼▼▼▼▼▼
            // file 객체에서 필요한 데이터를 추출하여 서비스에 전달
            fileStorageService.test("test");
            String fileName = fileStorageService.storeFile(file.getInputStream(), file.getOriginalFilename());

            String fileDownloadUri = fileStorageService.getFileDownloadUri(fileName);

            return ResponseEntity.ok(new FileResponse(fileName, fileDownloadUri, file.getContentType(), file.getSize()));

        } catch (IOException e) {
            logger.error("파일에서 InputStream을 얻는 데 실패했습니다.", e);
            // 적절한 에러 응답 반환
            return ResponseEntity.status(500).body(null);
        }
    }
}
