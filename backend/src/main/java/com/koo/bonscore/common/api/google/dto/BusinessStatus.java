package com.koo.bonscore.common.api.google.dto;

public enum BusinessStatus {
    OPERATIONAL,          // 정상 운영
    CLOSED_PERMANENTLY,   // 영구 폐업
    NO_INFO,              // 정보 없음
    NOT_FOUND,            // 가게 못찾음
    API_ERROR             // API 오류
}
