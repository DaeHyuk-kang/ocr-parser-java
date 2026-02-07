package com.kang.ocrparser.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VehicleNumberExtractor {

    // 예) 차량 번호: 5405
    // 예) 차량번호: 8713
    // 예) 차번호: 80구8713
    private static final Pattern VEHICLE_ANCHOR =
            Pattern.compile("(차량\\s*번호|차량번호|차\\s*번호|차번호)\\s*[:]?\\s*([^\\n\\r]{0,40})");

    // 흔한 번호판 패턴들(정교하게 하려면 더 늘릴 수 있음)
    private static final Pattern PLATE =
            Pattern.compile("([0-9]{2,3}[가-힣][0-9]{4}|[0-9]{4,5})");

    public static String extract(String rawText) {
        if (rawText == null || rawText.isBlank()) return null;

        Matcher anchor = VEHICLE_ANCHOR.matcher(rawText);
        if (anchor.find()) {
            String near = anchor.group(2);

            // "80구8713", "8713", "5405" 같은 걸 추출
            Matcher plate = PLATE.matcher(near.replaceAll("\\s+", ""));
            if (plate.find()) return plate.group(1);
        }

        // fallback: 전체에서라도 찾아보기 (라벨 없는 경우)
        Matcher plate = PLATE.matcher(rawText.replaceAll("\\s+", ""));
        if (plate.find()) return plate.group(1);

        return null;
    }
}
