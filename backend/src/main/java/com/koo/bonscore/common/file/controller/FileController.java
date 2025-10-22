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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public FileResponse uploadFile(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal UserDetails userDetail) {
        try {
            String originalFileName = file.getOriginalFilename();
            // 1. 파일을 '임시' 저장소에 저장합니다.
            String storedFileName = fileStorageService.storeTempFile(file.getInputStream(), originalFileName, userDetail.getUsername());

            // 2. 임시 파일에 접근할 필요는 없으므로, 다운로드 URI 대신 저장된 파일명만 응답해줘도 충분합니다.
            //    하지만 기존 DTO를 활용하기 위해 URI를 생성해서 보냅니다.
            String fileDownloadUri = fileStorageService.getTempFileDownloadUri(storedFileName);

            return new FileResponse(
                    originalFileName,
                    storedFileName,
                    fileDownloadUri,
                    file.getContentType(),
                    file.getSize()
            );

        } catch (IOException e) {
            logger.error("임시 파일 업로드 실패: {}", e.getMessage());
            // 실패 시 적절한 HTTP 상태 코드와 메시지를 반환하는 것이 좋습니다.
            throw new RuntimeException("파일 업로드에 실패했습니다.", e);
        }
    }
}
