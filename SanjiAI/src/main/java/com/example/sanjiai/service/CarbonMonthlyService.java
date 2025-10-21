package com.example.sanjiai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarbonMonthlyService {

    private final ReceiptCalculatorService calculator;
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * 월별 총 탄소 절감량 계산
     */
    public Map<String, Object> getMonthlySummary(Long userId, String month, List<Map<String, Object>> receipts) {
        int monthValue = Integer.parseInt(month);
        int year = LocalDate.now().getYear();

        List<CompletableFuture<Double>> futures = receipts.stream()
                .flatMap(receipt -> ((List<Map<String, Object>>) receipt.get("purchases")).stream()
                        .map(purchase -> CompletableFuture.supplyAsync(() -> {
                            String storeName = (String) receipt.get("storeName");
                            String productName = (String) purchase.get("productName");
                            double quantity = ((Number) purchase.get("quantity")).doubleValue();
                            double weightKg = quantity * 0.2;
                            return calculator.calculateSingle(storeName, productName, weightKg);
                        }, executor))
                )
                .collect(Collectors.toList());

        double totalSaved = futures.stream().mapToDouble(CompletableFuture::join).sum();
        int purchaseCount = futures.size();

        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("month", month);
        summary.put("year", year);
        summary.put("purchaseCount", purchaseCount);
        summary.put("totalSavedKg", roundTo3(totalSaved));
        return summary;
    }

    /**
     * 상품별 상세 탄소 절감량
     */
    public List<Map<String, Object>> getMonthlyDetail(Long userId, String month, List<Map<String, Object>> receipts) {
        List<CompletableFuture<Map<String, Object>>> futures = receipts.stream()
                .flatMap(receipt -> ((List<Map<String, Object>>) receipt.get("purchases")).stream()
                        .map(purchase -> CompletableFuture.supplyAsync(() -> {
                            String storeName = (String) receipt.get("storeName");
                            String productName = (String) purchase.get("productName");
                            double quantity = ((Number) purchase.get("quantity")).doubleValue();
                            double weightKg = quantity * 0.2;
                            double saved = calculator.calculateSingle(storeName, productName, weightKg);

                            Map<String, Object> detail = new HashMap<>();
                            detail.put("store", storeName);
                            detail.put("product", productName);
                            detail.put("quantity", quantity);
                            detail.put("savedKg", roundTo3(saved));
                            detail.put("date", receipt.get("receiptDate"));
                            return detail;
                        }, executor))
                )
                .collect(Collectors.toList());

        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    /**
     * 주차별 탄소 절감량 계산
     */
    public List<Map<String, Object>> getWeeklySummary(Long userId, String month, List<Map<String, Object>> receipts) {
        Map<Integer, Double> weekTotals = new HashMap<>();

        for (Map<String, Object> receipt : receipts) {
            LocalDate date = LocalDate.parse((String) receipt.get("receiptDate"));
            int day = date.getDayOfMonth();
            int weekNum = (day - 1) / 7 + 1;

            double totalForReceipt = 0.0;
            List<Map<String, Object>> purchases = (List<Map<String, Object>>) receipt.get("purchases");

            for (Map<String, Object> purchase : purchases) {
                String storeName = (String) receipt.get("storeName");
                String productName = (String) purchase.get("productName");
                double quantity = ((Number) purchase.get("quantity")).doubleValue();
                double weightKg = quantity * 0.2;
                totalForReceipt += calculator.calculateSingle(storeName, productName, weightKg);
            }

            weekTotals.put(weekNum, weekTotals.getOrDefault(weekNum, 0.0) + totalForReceipt);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int week = 1; week <= 4; week++) {
            Map<String, Object> data = new HashMap<>();
            data.put("week", week);
            data.put("savedKg", roundTo3(weekTotals.getOrDefault(week, 0.0)));
            result.add(data);
        }
        return result;
    }

    private double roundTo3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
