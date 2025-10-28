package com.example.sanjiai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 영수증 기반 탄소 절감량 계산 서비스 (AI 서비스 전용)
 *
 * 국내산 / 수입산 / 미상 원산지 상품의 운송 거리 및 운송 수단(트럭, 선박, 항공)을
 * 고려하여 절감 탄소량(kgCO2)을 계산합니다.
 */
@Service
@RequiredArgsConstructor
public class ReceiptCalculatorService {

    private final OpenAiService openai;
    private final CarbonService carbon;

    // 동일 구간 중복 호출 방지를 위한 캐시
    private final Map<String, Double> distanceCache = new ConcurrentHashMap<>();

    /**
     * 단일 품목 탄소 절감량 계산 (AI 기반, 디버깅 포함)
     */
    public double calculateSingle(String storeName, String productName, double weightKg) {
        double saved;
        try {
            // 1. 생산지 추정
            String originPrompt = String.format("%s의 주요 생산지를 알려줘. (국내 또는 국가 이름만)", productName);
            String origin = openai.ask(originPrompt);
//            System.out.println("[DEBUG] Origin Prompt: " + originPrompt);
//            System.out.println("[DEBUG] Origin Response: " + origin);

            // 2. 운송수단 추정 (origin 정보 포함)
            String transportPrompt;
            if (origin != null && (origin.contains("한국") || origin.contains("국내"))) {
                transportPrompt = String.format(
                        "%s는 보통 어떤 운송 수단(선박, 항공, 트럭)으로 운송되나요? 하나만 답변해.",
                        productName
                );
            } else {
                transportPrompt = String.format(
                        "%s는 %s(중 가장 가까운 지역)에서 한국으로 운송된다고 가정할 때, " +
                                "보통 어떤 운송 수단(선박, 항공, 트럭)으로 운송되나요? 하나만 답변해.",
                        productName, origin
                );
            }

            String transportMode = openai.ask(transportPrompt);
//            System.out.println("[DEBUG] Transport Prompt: " + transportPrompt);
//            System.out.println("[DEBUG] Transport Response (raw): " + transportMode);

            if (transportMode == null) transportMode = "";
            transportMode = transportMode.toLowerCase();

            // 운송수단 문자열 정규화
            if (transportMode.contains("air") || transportMode.contains("항공")) {
                transportMode = "항공";
            } else if (transportMode.contains("ship") || transportMode.contains("선박") || transportMode.contains("해상")) {
                transportMode = "선박";
            } else {
                transportMode = "트럭";
            }
//            System.out.println("[DEBUG] Transport Mode (parsed): " + transportMode);

            // 3. 거리 계산
            double distanceKm = estimateDistance(origin, storeName, transportMode);
//            System.out.println("[DEBUG] Estimated Distance (km): " + distanceKm);

            // 4. 운송 수단별 탄소 배출량 계산
            double cTransport;
            switch (transportMode) {
                case "항공":
                    cTransport = carbon.air(distanceKm, weightKg);
                    break;
                case "선박":
                    cTransport = carbon.ship(distanceKm, weightKg);
                    break;
                default:
                    cTransport = carbon.truck(distanceKm, weightKg);
            }
//            System.out.println("[DEBUG] cTransport: " + cTransport);

            // 5. 기준 푸드마일 대비 절감량 계산
            double cRef = carbon.referenceFoodMileage(weightKg);
            saved = Math.max(cRef - cTransport, 0.0);
//            System.out.println("[DEBUG] cRef: " + cRef + ", Saved: " + saved);

        } catch (Exception e) {
//            System.err.println("[ERROR] calculateSingle failed for product: " + productName);
            e.printStackTrace();
            saved = 0.0;
        }

        return roundTo3(saved);
    }

    /**
     * 산지와 판매점 간 거리 추정 (복수 국가 응답 처리 및 평균값 계산 포함)
     */
    private double estimateDistance(String origin, String destination, String mode) {
        String key = origin + "->" + destination + "(" + mode + ")";
        if (distanceCache.containsKey(key)) {
//            System.out.println("[DEBUG] Distance Cache Hit: " + key + " = " + distanceCache.get(key));
            return distanceCache.get(key);
        }

        if (origin == null || origin.isBlank()) {
            origin = "국내";
        }

        // 국내 또는 한국 포함 시 고정값 처리
        if (origin.contains("국내") || origin.contains("한국")) {
            double domesticDistance = 50.0;
            distanceCache.put(key, domesticDistance);
//            System.out.println("[DEBUG] Domestic origin detected. Distance = " + domesticDistance);
            return domesticDistance;
        }

        // 거리 질의 생성
        String prompt;
        if (mode.equals("항공")) {
            prompt = String.format("%s에서 한국까지의 직항 항공 거리(km)를 숫자만으로 알려줘.", origin);
        } else if (mode.equals("선박")) {
            prompt = String.format("%s에서 부산항까지의 주요 항로 거리(km)를 숫자만으로 알려줘.", origin);
        } else {
            prompt = String.format("%s와 %s 사이의 실제 도로 거리(km)를 숫자만으로 알려줘.", origin, destination);
        }

//        System.out.println("[DEBUG] Distance Prompt: " + prompt);
        String response = openai.ask(prompt);
//        System.out.println("[DEBUG] Distance Response: " + response);

        double result;

        try {
            // 여러 숫자 추출
            Matcher matcher = Pattern.compile("\\d+(?:\\.\\d+)?").matcher(response);
            List<Double> numbers = new ArrayList<>();
            while (matcher.find()) {
                numbers.add(Double.parseDouble(matcher.group()));
            }

            if (numbers.isEmpty()) {
                throw new NumberFormatException("No numeric data found");
            }

            // 평균값 계산
            double sum = 0.0;
            for (double n : numbers) sum += n;
            result = sum / numbers.size();

            // 과도한 값 방지
            if (result > 20000) result = 10000;

        } catch (Exception e) {
//            System.err.println("[WARN] Failed to parse distance, using fallback for " + key);
            result = mode.equals("항공") ? 3000.0 : 500.0;
        }

        distanceCache.put(key, result);
//        System.out.println("[DEBUG] Parsed Distance (km): " + result);
        return result;
    }

    /**
     * 소수점 3자리 반올림
     */
    private double roundTo3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
