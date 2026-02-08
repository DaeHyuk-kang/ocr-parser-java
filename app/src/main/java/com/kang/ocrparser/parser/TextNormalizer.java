package com.kang.ocrparser.parser;

public class TextNormalizer {

    public static String normalize(String rawText) {
        if (rawText == null) return "";

        String text = rawText.toLowerCase();

        // 시간 토큰 제거 (05:36 / 05:36:01 등)
        text = text.replaceAll("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\b", " ");

        // 1) kg 표기 통합 (k g, kG 등 → kg)
        text = text.replaceAll("k\\s*g", "kg");

        // 2) 숫자 사이 쉼표 제거 (14,080 → 14080)
        text = text.replaceAll("(?<=\\d),(?=\\d)", "");

        // 3) 공백 정리
        text = text.replaceAll("\\s+", " ");

        return text.trim();
    }
}
