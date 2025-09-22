package com.koo.bonscore.common.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("파일을 업로드할 디렉토리를 생성할 수 없습니다.", ex);
        }
    }

    /**
     * 파일을 저장하고 고유한 파일 이름을 반환합니다.
     * @param file 저장할 MultipartFile
     * @return 서버에 저장된 고유한 파일 이름
     */
    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String storedFileName = UUID.randomUUID().toString() + fileExtension;

        try {
            if (storedFileName.contains("..")) {
                throw new RuntimeException("파일 이름에 유효하지 않은 경로 시퀀스가 포함되어 있습니다: " + storedFileName);
            }
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return storedFileName;
        } catch (IOException ex) {
            throw new RuntimeException("파일 " + storedFileName + "을(를) 저장할 수 없습니다. 다시 시도해 주세요.", ex);
        }
    }

    /**
     * 파일 이름을 기반으로 접근 URL을 생성합니다.
     * @param fileName 파일 이름
     * @return 파일 접근 URL
     */
    public String getFileDownloadUri(String fileName) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/images/") // WebConfig에 설정된 경로와 일치해야 함
                .path(fileName)
                .toUriString();
    }
}
