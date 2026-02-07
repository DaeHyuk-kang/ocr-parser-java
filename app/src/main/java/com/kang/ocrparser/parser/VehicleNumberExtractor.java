package com.kang.ocrparser.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VehicleNumberExtractor {

    // 차량번호 라벨 확장: "차량 No. 0580" 대응
    private static final Pattern VEHICLE_ANCHOR =
            Pattern.compile("(?i)(차량\\s*(번호|no\\.?|넘버)?|차\\s*번호|차번호)\\s*[:.]?\\s*([^\\n\\r]{0,60})");

    // 번호판 패턴(한글 포함 우선), 숫자만(3~5자리)
    private static final Pattern PLATE =
            Pattern.compile("([0-9]{2,3}[가-힣][0-9]{4}|[0-9]{3,5})");

    // 도로명주소/연락처 라인 방어(04의 2960-19 같은 것)
    private static boolean looksLikeAddressOrContact(String line) {
        String l = line.toLowerCase();
        if (l.contains("tel") || l.contains("fax")) return true;
        // "...로 2960-19", "...길 123-4", "...번길 12-3" 같은 패턴
        if (line.matches(".*(로|길|번길)\\s*\\d+\\s*[-]\\s*\\d+.*")) return true;
        return false;
    }

    public static String extract(String rawText) {
        if (rawText == null || rawText.isBlank()) return null;

        // 1) 라벨 근처에서 우선 추출
        Matcher anchor = VEHICLE_ANCHOR.matcher(rawText);
        if (anchor.find()) {
            String near = anchor.group(3); // 라벨 뒤쪽
            String compact = near.replaceAll("\\s+", "");
            Matcher plate = PLATE.matcher(compact);
            if (plate.find()) return plate.group(1);
        }

        // 2) fallback: 전체에서 찾지 말고 "차량" 들어간 라인에서만 찾기
        for (String line : rawText.split("\\R")) {
            if (!(line.contains("차량") || line.toLowerCase().contains("vehicle"))) continue;
            if (looksLikeAddressOrContact(line)) continue;

            String compact = line.replaceAll("\\s+", "");
            Matcher plate = PLATE.matcher(compact);
            if (plate.find()) return plate.group(1);
        }

        return null;
    }
}
