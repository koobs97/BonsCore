package com.koo.bonscore.common.file.service;

import com.koo.bonscore.core.exception.custom.BsCoreException;
import com.koo.bonscore.core.exception.enumType.ErrorCode;
import com.koo.bonscore.core.exception.enumType.HttpStatusCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
@Service
public class FileStorageService {

    // 파일이 저장될 디렉토리의 경로를 나타내는 Path 객체
    private final Path fileStorageLocation;
    // 파일 다운로드 URI의 접두사를 저장할 변수
    private final String fileDownloadUriPrefix;
    // 업로드를 허용할 파일 확장자 목록 (이미지 파일)
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gibf", "webp");

    /**
     * FileStorageService 생성자.
     * application.yml에서 파일 관련 설정들을 주입받아 초기화
     *
     * @param uploadDir           application.yml의 'file.upload-dir' 값
     * @param fileDownloadUriPrefix application.yml의 'file.download-uri-prefix' 값
     */
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir,
                              @Value("${file.download-uri-prefix}") String fileDownloadUriPrefix) {

        // 업로드 디렉토리 경로 설정
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("디렉토리를 생성할 수 없습니다.", e);
        }

        // 파일 다운로드 URI 접두사 설정
        this.fileDownloadUriPrefix = fileDownloadUriPrefix;
    }

    /**
     * InputStream과 원본 파일 이름을 받아 파일을 서버에 저장
     * 파일 이름은 UUID를 사용하여 고유하게 생성
     *
     * @param inputStream      저장할 파일의 InputStream
     * @param originalFileName 사용자가 업로드한 원본 파일 이름
     * @return 서버에 저장된 고유한 파일 이름
     */
    public String storeFile(InputStream inputStream, String originalFileName) {

        // 원본파일명
        String cleanFileName = StringUtils.cleanPath(originalFileName);
        // 확장자 검사
        validateFileExtension(cleanFileName);
        // 확장자 추출
        String fileExtension = cleanFileName.substring(cleanFileName.lastIndexOf("."));
        // UUID를 사용한 새로운 파일 이름 생성
        String storedFileName = UUID.randomUUID().toString() + fileExtension;

        try {
            if (storedFileName.contains("..")) {
                throw new RuntimeException("파일 이름에 유효하지 않은 시퀀스가 포함되어 있습니다.");
            }
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);

            // 전달받은 InputStream을 사용하여 파일을 복사
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return storedFileName;
        } catch (IOException ex) {
            throw new RuntimeException(storedFileName + " 파일 저장에 실패했습니다.", ex);
        }
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
        if (dotIndex == -1 || dotIndex == 0) {
            throw new BsCoreException(
                    HttpStatusCode.BAD_REQUEST, // 잘못된 요청으로 처리
                    ErrorCode.INVALID_FILE_EXTENSION,
                    "파일 확장자가 유효하지 않습니다."
            );
        }

        // 확장자 추출 후 소문자로 변환
        String extension = fileName.substring(dotIndex + 1).toLowerCase();

        // 허용된 확장자 목록에 포함되어 있는지 확인
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BsCoreException(
                    HttpStatusCode.BAD_REQUEST, // 잘못된 요청으로 처리
                    ErrorCode.INVALID_FILE_EXTENSION,
                    "허용되지 않는 파일 확장자입니다. (허용: " + String.join(", ", ALLOWED_EXTENSIONS) + ")"
            );
        }
    }

    /**
     * 서버에 저장된 파일 이름을 받아 다운로드 가능한 전체 URI를 생성하여 반환
     *
     * @param fileName 서버에 저장된 파일 이름
     * @return 파일에 접근할 수 있는 전체 URL
     */
    public String getFileDownloadUri(String fileName) {
        return this.fileDownloadUriPrefix + fileName;
    }
}
