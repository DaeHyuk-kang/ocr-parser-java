package com.kang.ocrparser.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeighingDateExtractor {

    // 예) 계량일자: 2026-02-02 05:37:55
    // 예) 계량 일자: 2026-02-01 11:55:35
    // 예) 날 짜: 2026-02-02-00004  (뒤에 -00004 같은 식별자는 제거)
    private static final Pattern DATE_TIME =
            Pattern.compile("(\\d{4}-\\d{2}-\\d{2})(?:\\s+(\\d{1,2}:\\d{2}(?::\\d{2})?))?");

    private static final Pattern DATE_ANCHOR =
            Pattern.compile("(계량\\s*일자|계량일자|날\\s*짜|날짜)\\s*[:]?\\s*([^\\n\\r]{0,60})");

    public static String extract(String rawText) {
        if (rawText == null || rawText.isBlank()) return null;

        // 라벨(앵커) 근처에서만 추출: 근거 없는 전역 탐색은 오탐 위험이 커서 하지 않음
        Matcher anchor = DATE_ANCHOR.matcher(rawText);
        if (!anchor.find()) return null;

        String near = anchor.group(2);
        return pickDateTime(near);
    }

    private static String pickDateTime(String text) {
        if (text == null || text.isBlank()) return null;

        Matcher m = DATE_TIME.matcher(text);
        if (!m.find()) return null;

        String date = m.group(1);
        String time = m.group(2);

        if (date == null) return null;

        if (time == null || time.isBlank()) {
            return date; // 시간 없으면 날짜만
        }

        return date + " " + time;
    }
}
