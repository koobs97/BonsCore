package com.koo.bonscore.common.file.controller;

import com.koo.bonscore.common.file.dto.FileResponse;
import com.koo.bonscore.common.file.service.FileStorageService;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.enumType.HttpStatusCode;
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
    public FileResponse uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 파일을 저장하고, 서버에 저장된 파일명을 받습니다.
            String originalFileName = file.getOriginalFilename();
            String storedFileName = fileStorageService.storeFile(file.getInputStream(), originalFileName);

            // 2. 파일 다운로드 URI를 생성합니다.
            String fileDownloadUri = fileStorageService.getFileDownloadUri(storedFileName);

            // 3. 프론트엔드로 보낼 응답 객체를 생성합니다.
            FileResponse fileResponse = new FileResponse(
                    originalFileName,
                    storedFileName,
                    fileDownloadUri,
                    file.getContentType(),
                    file.getSize()
            );

            // 4. 생성된 응답 객체를 JSON 형태로 반환합니다. (HTTP 200 OK 상태와 함께)
            return fileResponse;

        } catch (IOException e) {
            // 예외 처리 (실패 시 적절한 응답 반환)
            // 간단하게는 Internal Server Error를 반환할 수 있습니다.
            logger.error(e.getMessage());
        }
        return null;
    }
}
