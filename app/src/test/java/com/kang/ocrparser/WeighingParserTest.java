package com.kang.ocrparser;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.kang.ocrparser.model.ParsedTicket;
import com.kang.ocrparser.parser.WeighingParser;

public class WeighingParserTest {

    @Test
    void parsesBasicWeighingTicket() {
        String ocrText = """
            계량일자: 2026-02-02 05:37:55
            차량번호: 80구8713
            총중량: 12480 kg
            공차중량: 7470 kg
            실중량: 5010 kg
            """;

        ParsedTicket ticket = WeighingParser.parse(ocrText);

        assertEquals("2026-02-02 05:37:55", ticket.getWeighingDate());
        assertEquals("80구8713", ticket.getVehicleNumber());
        assertEquals(12480, ticket.getGrossWeightKg());
        assertEquals(7470, ticket.getTareWeightKg());
        assertEquals(5010, ticket.getNetWeightKg());
    }
}
