package com.example.sanjiai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 탄소 배출량 계산 로직
 * 트럭, 선박, 항공 운송의 CO₂ 배출량(kg)을 계산.
 * 기준 푸드마일(400km 트럭 기준) 대비 절감량 산정에 활용.
 */
@Service
public class CarbonService {

    @Value("${app.carbon.truckKgPerTonKm:0.18}")
    private double truckKgPerTonKm;   // 트럭: 0.18 kgCO2 per ton·km

    @Value("${app.carbon.shipKgPerTonKm:0.012}")
    private double shipKgPerTonKm;    // 선박: 0.012 kgCO2 per ton·km

    @Value("${app.carbon.airKgPerTonKm:0.6}")
    private double airKgPerTonKm;     // 항공: 0.6 kgCO2 per ton·km (기본값, 평균 화물 항공기 기준)

    @Value("${app.carbon.referenceKm:400}")
    private double referenceKm;       // 기준 거리 (푸드마일 계산용)

    /**
     * 트럭 운송 시 kgCO₂ 계산
     */
    public double truck(double distanceKm, double weightKg) {
        return calculate(distanceKm, weightKg, truckKgPerTonKm);
    }

    /**
     * 선박 운송 시 kgCO₂ 계산
     */
    public double ship(double distanceKm, double weightKg) {
        return calculate(distanceKm, weightKg, shipKgPerTonKm);
    }

    /**
     * 항공 운송 시 kgCO₂ 계산
     */
    public double air(double distanceKm, double weightKg) {
        return calculate(distanceKm, weightKg, airKgPerTonKm);
    }

    /**
     * 기준 푸드마일 (400km 트럭 기준)
     */
    public double referenceFoodMileage(double weightKg) {
        return truck(referenceKm, weightKg);
    }

    /**
     * 공통 계산 메서드
     */
    private double calculate(double distanceKm, double weightKg, double factor) {
        if (distanceKm <= 0 || weightKg <= 0) return 0.0;
        double ton = weightKg / 1000.0;
        return distanceKm * ton * factor;
    }
}
