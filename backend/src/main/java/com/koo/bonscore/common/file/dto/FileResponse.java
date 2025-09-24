package com.koo.bonscore.common.file.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FileResponse {
    private String originalFileName; // 원본 파일명
    private String storedFileName;   // 서버에 저장된 파일명
    private String fileDownloadUri;  // 다운로드 URI
    private String fileType;         // 파일 타입 (e.g., "image/png")
    private long size;
}
