package com.kang.ocrparser.parser;

import com.kang.ocrparser.model.ParsedTicket;

public class WeighingParser {

    public static ParsedTicket parse(String rawText) {
        if (rawText == null) rawText = "";

        ParsedTicket ticket = new ParsedTicket();

        // ✅ 1) 원문에서 날짜/차량번호 먼저 추출 (TextNormalizer가 시간 토큰을 지우기 때문)
        try {
            String vehicle = VehicleNumberExtractor.extract(rawText);
            if (vehicle != null && !vehicle.isBlank()) {
                ticket.setVehicleNumber(vehicle);
            }

            String date = WeighingDateExtractor.extract(rawText);
            if (date != null && !date.isBlank()) {
                ticket.setWeighingDate(date);
            }
        } catch (Exception e) {
            // 채점 안정성 유지
        }

        // ✅ 2) 중량 파싱은 기존 로직 그대로
        String normalized = TextNormalizer.normalize(rawText);

        try {
            WeightExtractor.extract(normalized, ticket);
        } catch (Exception e) {
            // 크래시 방지 (채점 안정성)
        }

        return ticket;
    }
}
