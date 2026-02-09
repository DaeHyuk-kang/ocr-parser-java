package com.kang.ocrparser.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kang.ocrparser.model.ParsedTicket;

public class WeightExtractor {

    // 숫자: 130 / 14080 / 5 900 / 12480 / 13 460 등
    // - 1~3자리 + (공백/쉼표) + 3자리 반복 OR 그냥 1~6자리
    private static final String KG_NUM = "(\\d{1,3}(?:[\\s,]\\d{3})+|\\d{1,6})";

    // 라벨과 숫자 사이에 잡문자(시간은 TextNormalizer에서 제거됨)가 있어도 허용
    private static final String GAP = "[^0-9]{0,50}?";

    private static final int PATTERN_FLAGS = Pattern.MULTILINE;

    private static final Pattern GROSS_PATTERN =
            Pattern.compile("(총\\s*중\\s*량|총중량)\\s*[:]?\\s*" + GAP + KG_NUM + "\\s*kg", PATTERN_FLAGS);

    private static final Pattern TARE_PATTERN =
            Pattern.compile("(공\\s*차\\s*중\\s*량|공차중량|차\\s*중\\s*량|차중량)\\s*[:]?\\s*" + GAP + KG_NUM + "\\s*kg", PATTERN_FLAGS);

    private static final Pattern NET_PATTERN =
            Pattern.compile("(실\\s*중\\s*량|실중량)\\s*[:]?\\s*" + GAP + KG_NUM + "\\s*kg", PATTERN_FLAGS);

    private static final Pattern ANY_KG_PATTERN =
            Pattern.compile(KG_NUM + "\\s*kg", PATTERN_FLAGS);

    public static void extract(String normalizedText, ParsedTicket ticket) {
        if (normalizedText == null || normalizedText.isEmpty() || ticket == null) return;

        // 1) normalizedText을 확인해서 문자열 값을 확인하고 그 안에 GROSS_PATTERN과 일치하는 값이 있으면 setGrossWeightKg에 저장
        setIfFound(GROSS_PATTERN, normalizedText, ticket::setGrossWeightKg);
        setIfFound(TARE_PATTERN, normalizedText, ticket::setTareWeightKg);
        setIfFound(NET_PATTERN, normalizedText, ticket::setNetWeightKg);

        boolean needFallback =
                ticket.getGrossWeightKg() == null
                        || ticket.getTareWeightKg() == null
                        || ticket.getNetWeightKg() == null;

        if (needFallback) {
            List<Integer> weights = new ArrayList<>();

            Matcher all = ANY_KG_PATTERN.matcher(normalizedText);
            while (all.find()) {
                String rawNum = all.group(1);

                // ✅ 시간/콜론 근처 오염만 최소한으로 차단,회피
                if (looksLikeTimeNoiseAround(normalizedText, all.start(), rawNum)) {
                    continue;
                }

                Integer w = parseKg(rawNum);
                if (w == null) continue;

                // 너무 비현실적인 값만 배제
                if (w < 1 || w > 300_000) continue;

                weights.add(w);
            }

            if (!weights.isEmpty()) {
                // 이미 확정된 gross/tare는 후보에서 제거 (net은 조합검증에 필요할 수 있어 유지)
                removeIfEquals(weights, ticket.getGrossWeightKg());
                removeIfEquals(weights, ticket.getTareWeightKg());

                weights = distinctPreserveOrder(weights);
                Collections.sort(weights);

                // 2-1) 조합 검증 우선: gross > tare, net = gross - tare
                fillByConsistentTriple(weights, ticket);

                // 2-2) 그래도 비면 휴리스틱 채우기
                List<Integer> weightsForHeuristic = new ArrayList<>(weights);
                removeIfEquals(weightsForHeuristic, ticket.getNetWeightKg());
                Collections.sort(weightsForHeuristic);

                // gross 비면 최대값
                if (ticket.getGrossWeightKg() == null && !weightsForHeuristic.isEmpty()) {
                    ticket.setGrossWeightKg(weightsForHeuristic.get(weightsForHeuristic.size() - 1));
                }

                // tare 비면: gross & net이 있으면 (net, gross) 사이 값을 선택
                if (ticket.getTareWeightKg() == null) {
                    Integer g = ticket.getGrossWeightKg();
                    Integer n = ticket.getNetWeightKg();

                    if (g != null && n != null) {
                        for (int v : weightsForHeuristic) {
                            if (v > n && v < g) {
                                ticket.setTareWeightKg(v);
                                break;
                            }
                        }
                    }

                    // 마지막 안전장치: 최소값 (단, 후보가 2개 이상일 때)
                    if (ticket.getTareWeightKg() == null && weightsForHeuristic.size() >= 2) {
                        ticket.setTareWeightKg(weightsForHeuristic.get(0));
                    }
                }
            }
        }

        // 3-0) 공차중량이 없지만 gross/net이 있으면 tare = gross - net 로 복구
        if (ticket.getTareWeightKg() == null
                && ticket.getGrossWeightKg() != null
                && ticket.getNetWeightKg() != null) {

            int computedTare = ticket.getGrossWeightKg() - ticket.getNetWeightKg();
            if (computedTare > 0) ticket.setTareWeightKg(computedTare);
        }

        // 3) 실중량이 없으면 gross - tare 계산
        if (ticket.getNetWeightKg() == null
                && ticket.getGrossWeightKg() != null
                && ticket.getTareWeightKg() != null) {

            int computedNet = ticket.getGrossWeightKg() - ticket.getTareWeightKg();
            if (computedNet > 0) ticket.setNetWeightKg(computedNet);
        }
    }

    private static void fillByConsistentTriple(List<Integer> candidates, ParsedTicket ticket) {
        Integer g0 = ticket.getGrossWeightKg();
        Integer t0 = ticket.getTareWeightKg();
        Integer n0 = ticket.getNetWeightKg();

        List<Integer> pool = new ArrayList<>(candidates);
        if (g0 != null) pool.add(g0);
        if (t0 != null) pool.add(t0);
        if (n0 != null) pool.add(n0);

        pool = distinctPreserveOrder(pool);
        Collections.sort(pool);

        Set<Integer> poolSet = new HashSet<>(pool);

        for (int i = pool.size() - 1; i >= 0; i--) {
            int gross = pool.get(i);
            for (int j = 0; j < pool.size(); j++) {
                int tare = pool.get(j);
                if (gross <= tare) continue;

                int net = gross - tare;
                if (!poolSet.contains(net)) continue;

                if (g0 != null && g0 != gross) continue;
                if (t0 != null && t0 != tare) continue;
                if (n0 != null && n0 != net) continue;

                if (ticket.getGrossWeightKg() == null) ticket.setGrossWeightKg(gross);
                if (ticket.getTareWeightKg() == null) ticket.setTareWeightKg(tare);
                if (ticket.getNetWeightKg() == null) ticket.setNetWeightKg(net);
                return;
            }
        }
    }

    private static void setIfFound(Pattern p, String text, java.util.function.Consumer<Integer> setter) {
        Matcher m = p.matcher(text);
        if (m.find()) {
            // (라벨)(숫자) => group(2)가 숫자
            Integer v = parseKg(m.group(2));
            if (v != null) setter.accept(v);
        }
    }

    private static void removeIfEquals(List<Integer> list, Integer v) {
        if (v == null) return;
        list.removeIf(x -> x != null && x.equals(v));
    }

    private static Integer parseKg(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;

        // 쉼표 제거
        s = s.replace(",", "");

        // 천 단위 공백/탭 등 제거
        s = s.replaceAll("\\s+", "");

        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<Integer> distinctPreserveOrder(List<Integer> list) {
        List<Integer> out = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (Integer v : list) {
            if (v == null) continue;
            if (seen.add(v)) out.add(v);
        }
        return out;
    }

    /*
     * ocr텍스트의 문제를 텍스트노멀라이저에서 해결을 못했을때 (시간 사이 줄바꿈,시.분.초)
     * looksLikeTimeNoiseAround에서 시간관련 숫자들을 차단하여 회피하는방식으로 구성
     * (시간 자체는 TextNormalizer에서 제거하지만, OCR 깨짐으로 콜론이 남는 경우 방어)
     */
    private static boolean looksLikeTimeNoiseAround(String text, int matchStart, String rawNum) {
        if (text == null || rawNum == null) return false;

        // 공백 포함 숫자에서 주로 문제 발생 (예: "18 997")
        if (!rawNum.contains(" ")) return false;

        int from = Math.max(0, matchStart - 25);
        String left = text.substring(from, matchStart);

        // 예: "... 05:26: 18 997 kg" / "... 05:26 18 997 kg"
        if (left.matches(".*\\d{1,2}:\\d{2}\\s*$")) return true;
        if (left.matches(".*\\d{1,2}:\\s*$")) return true;

        return false;
    }
}
