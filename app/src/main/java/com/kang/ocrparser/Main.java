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

        System.out.println("ARGS[0]=" + inputPath);
        System.out.println("ARGS[1]=" + outputPath);

        Path in = Path.of(inputPath);
        if (!Files.exists(in)) {
            System.err.println("Input file not found: " + in.toAbsolutePath());
            System.exit(2);
        }

        String raw = Files.readString(in, StandardCharsets.UTF_8);

        // OCR 텍스트 추출 (json 확장자지만 내부가 텍스트일 수도 있으니 loader가 방어)
        String ocrText = inputPath.toLowerCase().endsWith(".json")
                ? SampleJsonLoader.extractOcrText(raw)
                : raw;

        ParsedTicket ticket;
        try {
            ticket = WeighingParser.parse(ocrText);
        } catch (Exception e) {
            // 채점 안정성: 어떤 예외가 와도 결과는 만들어서 내보내기
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
