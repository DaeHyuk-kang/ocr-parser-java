package com.kang.ocrparser.io;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SampleJsonLoader {

    private static final ObjectMapper OM = new ObjectMapper();

    private static final String[] CANDIDATE_KEYS = {
            "text", "ocr_text", "ocrText", "raw_text", "rawText", "content", "result", "data"
    };

    private static final int MAX_CANDIDATE_CHARS = 30_000;
    private static final int MAX_SCORE_CHARS = 2_000;

    /**
     * 샘플 JSON 문자열에서 OCR 텍스트를 robust하게 찾아 반환
     * - 절대 throw 하지 않게 만들어서 Main이 안정적으로 동작하도록 함
     */
    public static String extractOcrText(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) return "";

        JsonNode root;
        try {
            root = OM.readTree(jsonString);
        } catch (Exception e) {
            // 확장자는 json인데 내용이 텍스트일 수도 있음
            return jsonString;
        }

        Set<String> candidates = new LinkedHashSet<>();
        collectTextCandidates(root, candidates);

        if (candidates.isEmpty()) return "";

        String best = "";
        int bestScore = Integer.MIN_VALUE;

        for (String c : candidates) {
            int score = scoreAsOcrText(c);
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    private static void collectTextCandidates(JsonNode node, Collection<String> out) {
        if (node == null) return;

        if (node.isTextual()) {
            String v = node.asText();
            if (v == null) return;

            v = v.trim();
            if (v.isEmpty()) return;

            if (v.length() > MAX_CANDIDATE_CHARS) {
                v = v.substring(0, MAX_CANDIDATE_CHARS);
            }
            out.add(v);
            return;
        }

        if (node.isObject()) {
            // 유력 키 우선 탐색
            for (String key : CANDIDATE_KEYS) {
                JsonNode v = node.get(key);
                if (v != null) collectTextCandidates(v, out);
            }

            // 전체 필드 탐색
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> e = fields.next();
                collectTextCandidates(e.getValue(), out);
            }
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) collectTextCandidates(child, out);
        }
    }

    private static int scoreAsOcrText(String s) {
        if (s == null) return Integer.MIN_VALUE;

        int score = 0;
        String head = (s.length() > MAX_SCORE_CHARS) ? s.substring(0, MAX_SCORE_CHARS) : s;
        String lower = head.toLowerCase();

        // 길이 점수
        score += Math.min(head.length(), MAX_SCORE_CHARS) / 10;

        // 키워드 점수
        if (lower.contains("kg")) score += 50;
        if (lower.contains("총") && (lower.contains("중량") || lower.contains("중 량"))) score += 40;
        if (lower.contains("공차") || lower.contains("차중량") || lower.contains("차 중량")) score += 35;
        if (lower.contains("실") && lower.contains("중량")) score += 35;

        // 줄바꿈(영수증 형태) 점수
        int newlines = 0;
        for (int i = 0; i < head.length(); i++) if (head.charAt(i) == '\n') newlines++;
        score += Math.min(newlines, 50) * 2;

        // HTML/스크립트 같은 건 감점
        if (lower.contains("<html") || lower.contains("<body") || lower.contains("<script") || lower.contains("</")) {
            score -= 120;
        }

        // 숫자 많으면 가중치
        int digits = 0;
        for (int i = 0; i < head.length(); i++) {
            char ch = head.charAt(i);
            if (ch >= '0' && ch <= '9') digits++;
        }
        score += Math.min(digits, 200) / 2;

        if (head.length() < 20) score -= 30;

        return score;
    }
}
