package com.kang.ocrparser.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SampleJsonLoader {

    private static final ObjectMapper OM = new ObjectMapper();

    /*
     * sample JSON에서 OCR text를 최대한 안전하게 뽑는다.
     * - {"text": "..."} 형태면 text 사용
     * - 그 외 구조면 가능한 케이스(ocr.text/data.text/pages[*].text)까지 확인
     * - 그래도 못 찾으면 raw를 텍스트로 간주
     */
    public static String extractOcrText(String rawJsonOrText) {
        if (rawJsonOrText == null) return "";

        String s = rawJsonOrText.trim();
        if (s.isEmpty()) return "";

        // JSON이 아닌 경우: 그대로 텍스트로 사용
        if (!(s.startsWith("{") || s.startsWith("["))) {
            return rawJsonOrText;
        }

        try {
            JsonNode root = OM.readTree(s);

            // 1) 최우선: top-level "text"
            JsonNode text = root.get("text");
            if (text != null && text.isTextual()) {
                return text.asText("");
            }

            // 2) {"ocr": {"text": "..."}}, {"data": {"text": "..."}}
            JsonNode ocr = root.get("ocr");
            if (ocr != null && ocr.has("text") && ocr.get("text").isTextual()) {
                return ocr.get("text").asText("");
            }
            JsonNode data = root.get("data");
            if (data != null && data.has("text") && data.get("text").isTextual()) {
                return data.get("text").asText("");
            }

            // 3) pages[*].text (샘플 구조 대응)
            JsonNode pages = root.get("pages");
            if (pages != null && pages.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode page : pages) {
                    if (page != null && page.has("text") && page.get("text").isTextual()) {
                        String t = page.get("text").asText("").trim();
                        if (!t.isEmpty()) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append(t);
                        }
                    }
                }
                if (sb.length() > 0) {
                    return sb.toString();
                }
            }

            // 4) 아무것도 못 찾으면 raw를 텍스트로 취급
            return rawJsonOrText;

        } catch (Exception e) {
            // 파싱 실패면 raw를 텍스트로 사용
            return rawJsonOrText;
        }
    }
}
