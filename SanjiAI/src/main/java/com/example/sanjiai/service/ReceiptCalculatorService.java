package com.example.sanjiai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
     * 단일 품목 탄소 절감량 계산
     */
    public double calculateSingle(String storeName, String productName, double weightKg) {
        double saved;

        // GPT로 주요 산지 및 운송 방식 추정
        String origin = openai.ask(
                String.format("%s의 주요 생산지를 알려줘. (국내 또는 국가 이름만)", productName)
        );
        String transportMode = openai.ask(
                String.format("%s는 보통 어떤 운송 수단(선박, 항공, 트럭)으로 운송되나요? 하나만 답변해.", productName)
        ).replaceAll("[^가-힣a-zA-Z]", "").toLowerCase();

        // 거리 계산 (항공, 선박, 트럭 구분)
        double distanceKm = estimateDistance(origin, storeName, transportMode);

        // 수단별 탄소 배출 계산
        double cTransport;
        switch (transportMode) {
            case "항공":
            case "air":
                cTransport = carbon.air(distanceKm, weightKg);
                break;
            case "선박":
            case "ship":
            case "해상":
                cTransport = carbon.ship(distanceKm, weightKg);
                break;
            default:
                cTransport = carbon.truck(distanceKm, weightKg);
        }

        // 기준 푸드마일 대비 절감량 계산
        double cRef = carbon.referenceFoodMileage(weightKg);
        saved = Math.max(cRef - cTransport, 0.0);

        return roundTo3(saved);
    }

    /**
     * 산지와 판매점 간 거리 추정
     */
    private double estimateDistance(String origin, String destination, String mode) {
        String key = origin + "->" + destination + "(" + mode + ")";
        if (distanceCache.containsKey(key)) {
            return distanceCache.get(key);
        }

        String prompt;
        if (mode.contains("항공") || mode.contains("air")) {
            prompt = String.format("%s에서 %s까지의 직항 항공 거리(km)를 숫자만으로 알려줘.", origin, destination);
        } else if (mode.contains("선박") || mode.contains("ship") || mode.contains("해상")) {
            prompt = String.format("%s에서 %s까지의 주요 항로 거리(km)를 숫자만으로 알려줘.", origin, "부산항");
        } else {
            prompt = String.format("%s와 %s 사이의 실제 도로 거리(km)를 숫자만으로 알려줘.", origin, destination);
        }

        String response = openai.ask(prompt);
        double result;
        try {
            result = Double.parseDouble(response.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            result = mode.contains("항공") ? 3000.0 : 500.0;
        }

        distanceCache.put(key, result);
        return result;
    }

    private double roundTo3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
