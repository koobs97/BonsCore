package com.koo.bonscore.common.file.service;

import com.koo.bonscore.core.config.enc.EncryptionService;
import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.enumType.HttpStatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * <pre>
 * FileStorageService.java
 * 설명 : 파일 저장, 관리 및 관련 유틸리티 기능을 제공하는 서비스 클래스
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-09-22
 */
@Slf4j
@Service
public class FileStorageService {

    private final Path fileStorageLocation;     // 파일이 저장될 디렉토리의 경로를 나타내는 Path 객체
    private final String fileDownloadUriPrefix; // 파일 다운로드 URI의 접두사를 저장할 변수
    private final Path tempFileStorageLocation;
    private final String tempFileDownloadUriPrefix;
    private final EncryptionService encryptionService;

    // 업로드를 허용할 파일 확장자 목록 (이미지 파일)
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    /**
     * FileStorageService 생성자.
     * application.yml에서 파일 관련 설정들을 주입받아 초기화
     *
     * @param uploadDir             application.yml의 'file.upload-dir' 값
     * @param tempUploadDir         임시 경로
     * @param fileDownloadUriPrefix application.yml의 'file.download-uri-prefix' 값
     */
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir,
                              @Value("${file.upload-dir-temp}") String tempUploadDir,
                              @Value("${file.download-uri-prefix}") String fileDownloadUriPrefix,
                              @Value("${file.temp-download-uri-prefix}") String tempFileDownloadUriPrefix,
                              EncryptionService encryptionService
    ) {

        // 업로드 디렉토리 경로 설정
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.tempFileStorageLocation = Paths.get(tempUploadDir).toAbsolutePath().normalize(); // 임시 경로 초기화
        this.fileDownloadUriPrefix = fileDownloadUriPrefix;
        this.tempFileDownloadUriPrefix = tempFileDownloadUriPrefix;
        this.encryptionService = encryptionService;

        try {
            Files.createDirectories(this.fileStorageLocation);
            Files.createDirectories(this.tempFileStorageLocation); // 임시 디렉토리 생성
        } catch (IOException e) {
            throw new RuntimeException("디렉토리를 생성할 수 없습니다.", e);
        }
    }

    /**
     * 파일을 '임시' 저장소에 저장.
     * @param inputStream      저장할 파일의 InputStream
     * @param originalFileName 사용자가 업로드한 원본 파일 이름
     * @return 서버에 저장된 고유한 파일 이름
     */
    public String storeTempFile(InputStream inputStream, String originalFileName, String userId) {
        String storedFileName = createStoredFileName(originalFileName, userId);

        try {
            // 파일을 임시 저장소 위치에 저장
            Path targetLocation = this.tempFileStorageLocation.resolve(storedFileName);
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return storedFileName;
        } catch (IOException ex) {
            throw new RuntimeException(storedFileName + " 임시 파일 저장에 실패했습니다.", ex);
        }
    }

    /**
     * 임시 저장소에 있는 파일을 영구 저장소로 이동시킵니다.
     * @param storedFileName 임시 저장소에 있는 파일명
     * @param permanentSubPath 영구 저장소의 하위 경로 (예: "posts/123")
     * @return 영구 저장소의 상대 경로를 포함한 파일명
     */
    public String moveToPermanentStorage(String storedFileName, String permanentSubPath) throws IOException {
        Path sourcePath = this.tempFileStorageLocation.resolve(storedFileName);

        if (!Files.exists(sourcePath)) {
            // 파일이 임시 저장소에 없으면 예외 처리
            throw new IOException("임시 파일을 찾을 수 없습니다: " + storedFileName);
        }

        // 영구 저장 경로 생성 (예: C:/bons/BonsCore/uploads/posts/123)
        Path permanentDir = this.fileStorageLocation.resolve(permanentSubPath);
        Files.createDirectories(permanentDir);
        Path destinationPath = permanentDir.resolve(storedFileName);

        // 파일 이동
        Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

        // 최종적으로 저장된 상대 경로 반환 (예: posts/123/uuid-filename.jpg)
        return Paths.get(permanentSubPath).resolve(storedFileName).toString().replace("\\", "/");
    }

    /**
     * 원본 파일명을 기반으로 고유한 저장용 파일명을 생성합니다.
     */
    private String createStoredFileName(String originalFileName, String userId) {
        String cleanFileName = StringUtils.cleanPath(originalFileName);
        validateFileExtension(cleanFileName);
        String fileExtension = cleanFileName.substring(cleanFileName.lastIndexOf("."));
        return encryptionService.hashString(userId) + "_" + UUID.randomUUID() + fileExtension;
    }

    /**
     * 파일의 확장자를 검사하여 허용된 확장자인지 확인
     * 허용되지 않은 경우 BsCoreException을 발생
     *
     * @param fileName 검사할 파일의 전체 이름
     */
    private void validateFileExtension(String fileName) {
        // 파일 이름에서 마지막 '.'의 위치를 찾음
        int dotIndex = fileName.lastIndexOf('.');

        // '.'이 없거나, 파일 이름이 '.'으로 시작하는 경우 (예: .bashrc)
        if (dotIndex == -1 || dotIndex == 0)
            throw new BsCoreException(ErrorCode.INVALID_FILE_EXTENSION);

        // 확장자 추출 후 소문자로 변환
        String extension = fileName.substring(dotIndex + 1).toLowerCase();

        // 허용된 확장자 목록에 포함되어 있는지 확인
        if (!ALLOWED_EXTENSIONS.contains(extension))
            throw new BsCoreException(ErrorCode.INVALID_FILE_EXTENSION);
    }

    /**
     * 서버에 저장된 파일 이름을 받아 다운로드 가능한 전체 URI를 생성하여 반환
     *
     * @param storedRelativePath 상대 경로
     * @return 파일에 접근할 수 있는 전체 URL
     */
    public String getFileDownloadUri(String storedRelativePath) {
        return this.fileDownloadUriPrefix + storedRelativePath;
    }

    /**
     *
     * @param tempFileName  파일명만
     * @return
     */
    public String getTempFileDownloadUri(String tempFileName) {
        return this.tempFileDownloadUriPrefix + tempFileName;
    }

    /**
     * 특정 사용자가 남긴 모든 임시 파일을 삭제한다.
     * 파일명은 'hashedUserId_*' 패턴을 사용하여 검색.
     * 사용자가 로그인 할 때 한번 호출
     *
     * @param userId 정리할 대상 사용자의 원본 ID
     */
    public void cleanupUserTempFiles(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("사용자 ID가 제공되지 않아 임시 파일 정리를 건너뜁니다.");
            return;
        }

        // 1. 사용자 ID를 해싱하여 파일명 접두사를 만듭니다.
        String hashedUserId = encryptionService.hashString(userId);
        log.info("'{}' 사용자의 임시 파일 정리를 시작합니다. (파일 접두사: {}...)", userId, hashedUserId.substring(0, 10));

        int deleteCount = 0;
        // 2. DirectoryStream에 glob 패턴을 사용하여 해당 사용자의 파일만 필터링합니다.
        //    패턴: "해시값_*"
        String glob = hashedUserId + "_*";

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempFileStorageLocation, glob)) {
            for (Path filePath : stream) {
                // isRegularFile 체크는 안전을 위해 유지하는 것이 좋습니다.
                if (Files.isRegularFile(filePath)) {
                    try {
                        Files.delete(filePath);
                        log.debug("삭제된 임시 파일: {}", filePath.getFileName());
                        deleteCount++;
                    } catch (IOException e) {
                        // 파일 하나를 삭제하지 못하더라도 전체 작업이 중단되지 않도록 예외 처리
                        log.error("임시 파일 삭제 실패: {}", filePath.getFileName(), e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("임시 디렉토리에서 '{}' 패턴의 파일을 찾는 중 오류 발생", glob, e);
        }

        log.info("'{}' 사용자의 임시 파일 정리 완료. 총 {}개의 파일 삭제.", userId, deleteCount);
    }
}
