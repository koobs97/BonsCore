package com.koo.bonscore.common.file.service;

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
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    // 생성자는 원래대로 복구 (@Value 사용)
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        System.err.println("### FileStorageService 생성자 호출! ###");
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("디렉토리를 생성할 수 없습니다.", e);
        }
    }

    public void test(String fileName) {
        System.err.println("### test 메소드 호출 성공! ###");
    }

    // ▼▼▼▼▼▼▼ 핵심 수정 부분 ▼▼▼▼▼▼▼
    // MultipartFile 대신 InputStream과 originalFileName을 받도록 변경
    public String storeFile(InputStream inputStream, String originalFileName) {
        System.err.println("### storeFile 메소드 호출 성공! ###");

        String cleanFileName = StringUtils.cleanPath(originalFileName);
        String fileExtension = cleanFileName.substring(cleanFileName.lastIndexOf("."));
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

    public String getFileDownloadUri(String fileName) {
        return "http://localhost:8080/images/" + fileName;
    }
}
