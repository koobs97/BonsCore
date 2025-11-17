package com.koo.bonscore.common.api.google.service;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoogleTranslateService {

    // 1. Google 번역 클라이언트 인스턴스를 생성합니다.
    //    환경 변수가 올바르게 설정되었다면, 라이브러리가 자동으로 인증 정보를 찾아 연결합니다.
    private final Translate translate = TranslateOptions.getDefaultInstance().getService();

    /**
     * 여러 개의 텍스트를 한 번에 번역합니다. (이것이 더 효율적입니다)
     * @param texts 번역할 텍스트 목록 (예: ["다운타우너 안국", "런던 베이글 뮤지엄"])
     * @param sourceLang 원본 언어 코드 (예: "ko")
     * @param targetLang 목표 언어 코드 (예: "en")
     * @return 번역된 텍스트 목록
     */
    public List<String> translateTexts(List<String> texts, String sourceLang, String targetLang) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 2. 번역 API를 호출합니다.
            List<Translation> translations = translate.translate(texts,
                    Translate.TranslateOption.sourceLanguage(sourceLang),
                    Translate.TranslateOption.targetLanguage(targetLang));

            // 3. 결과 객체에서 번역된 텍스트만 추출하여 리스트로 만듭니다.
            return translations.stream()
                    .map(Translation::getTranslatedText)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Google 번역 API 호출 중 오류 발생: {}", e.getMessage(), e);
            // 번역 실패 시 비어있는 리스트를 반환하거나, 예외를 던질 수 있습니다.
            return Collections.emptyList();
        }
    }
}