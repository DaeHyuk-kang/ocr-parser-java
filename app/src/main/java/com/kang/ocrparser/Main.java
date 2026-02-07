package com.kang.ocrparser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kang.ocrparser.io.SampleJsonLoader;
import com.kang.ocrparser.model.ParsedTicket;
import com.kang.ocrparser.parser.WeighingParser;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args == null || args.length < 2) {
            System.out.println("Usage:");
            System.out.println("  ./gradlew :app:run --args=\"<input.(txt|json)> <output.json>\"");
            System.exit(1);
        }

        String inputPath = args[0];
        String outputPath = args[1];

        System.out.println("[START] OCR Weighing Ticket Parser");
        System.out.println("[INPUT]  file = " + inputPath);
        System.out.println("[OUTPUT] file = " + outputPath);

        Path in = Path.of(inputPath);
        if (!Files.exists(in)) {
            System.err.println("[ERROR] Input file not found: " + in.toAbsolutePath());
            System.exit(2);
        }

        // 1) 파일 읽기
        String raw = Files.readString(in, StandardCharsets.UTF_8);

        // 2) JSON이면 OCR text만 추출, 아니면 그대로 사용
        String ocrText = inputPath.toLowerCase().endsWith(".json")
                ? SampleJsonLoader.extractOcrText(raw)
                : raw;

        // 3) OCR 전처리 (입력 안정성 확보)
        // (1) BOM 제거
        ocrText = ocrText.replace("\uFEFF", "");

        // (2) 제로폭 문자 제거 (OCR / JSON에서 간혹 섞임)
        ocrText = ocrText.replaceAll("[\\u200B-\\u200D\\u2060]", "");

        // (3) 줄바꿈 통일 (Windows / Unix 차이 제거)
        ocrText = ocrText.replace("\r\n", "\n").replace("\r", "\n");

        System.out.println("[INFO] OCR text extracted (length=" + ocrText.length() + ")");
        System.out.println("[RAW_HEAD] " +
                ocrText.substring(0, Math.min(300, ocrText.length()))
                        .replace("\r", "\\r")
                        .replace("\n", "\\n")
        );

        // 4) 파싱
        ParsedTicket ticket;
        try {
            ticket = WeighingParser.parse(ocrText);
            System.out.println("[INFO] Parsing completed");
        } catch (Exception e) {
            // 어떤 예외가 와도 결과는 만들어서 내보내기
            System.err.println("[WARN] Parsing failed, returning empty result: "
                    + e.getClass().getSimpleName());
            ticket = new ParsedTicket();
        }

        // 5) 출력 디렉토리 자동 생성
        Path out = Path.of(outputPath);
        Path parent = out.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // 6) JSON 출력
        ObjectMapper om = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);

        Files.writeString(out, om.writeValueAsString(ticket), StandardCharsets.UTF_8);
        System.out.println("Saved: " + out.toAbsolutePath());
    }
}
