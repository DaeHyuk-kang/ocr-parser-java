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

        String raw = Files.readString(in, StandardCharsets.UTF_8);

        // OCR 텍스트 추출 (json 확장자지만 내부가 텍스트일 수도 있으니 loader가 방어)
        String ocrText = inputPath.toLowerCase().endsWith(".json")
                ? SampleJsonLoader.extractOcrText(raw)
                : raw;

        System.out.println("[INFO] OCR text extracted (length=" + ocrText.length() + ")");
        
        ParsedTicket ticket;
     // (1) BOM 제거
        ocrText = ocrText.replace("\uFEFF", "");

        // (2) 제로폭 문자 제거(가끔 OCR/JSON에서 섞임)
        ocrText = ocrText.replaceAll("[\\u200B-\\u200D\\u2060]", "");

        // (3) 줄바꿈 통일 (Windows CRLF/LF 차이 제거)
        ocrText = ocrText.replace("\r\n", "\n").replace("\r", "\n");
        System.out.println("[INFO] OCR text extracted (length=" + ocrText.length() + ")");
        System.out.println("[RAW_HEAD] " +
                ocrText.substring(0, Math.min(300, ocrText.length()))
                      .replace("\r","\\r").replace("\n","\\n")
        );
        try {
            ticket = WeighingParser.parse(ocrText);
            System.out.println("[INFO] Parsing completed");
        } catch (Exception e) {
            // 어떤 예외가 와도 결과는 만들어서 내보내기
            System.err.println("[WARN] Parsing failed, returning empty result: "
                    + e.getClass().getSimpleName());
            ticket = new ParsedTicket();
        }

        // 출력 디렉토리 자동 생성
        Path out = Path.of(outputPath);
        Path parent = out.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        Files.writeString(out, om.writeValueAsString(ticket), StandardCharsets.UTF_8);

        System.out.println("Saved: " + out.toAbsolutePath());
    }
}
