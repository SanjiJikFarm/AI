package com.example.sanjiai.controller;

import com.example.sanjiai.service.CarbonMonthlyService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/carbon")
@RequiredArgsConstructor
public class CarbonController {

    private final CarbonMonthlyService monthlyService;

    @PostMapping("/monthly")
    @Operation(summary = "달 별 탄소 절감량 계산")
    public Map<String, Object> getMonthlySummary(
            @RequestParam Long userId,
            @RequestParam String month,
            @RequestBody List<Map<String, Object>> receipts
    ) {
        return monthlyService.getMonthlySummary(userId, month, receipts);
    }

    @PostMapping("/product")
    @Operation(summary = "상품별 탄소 절감량 계산")
    public List<Map<String, Object>> getMonthlyDetail(
            @RequestParam Long userId,
            @RequestParam String month,
            @RequestBody List<Map<String, Object>> receipts
    ) {
        return monthlyService.getMonthlyDetail(userId, month, receipts);
    }

    @PostMapping("/weekly")
    @Operation(summary = "주별 탄소 절감량 계산")
    public List<Map<String, Object>> getWeeklySummary(
            @RequestParam Long userId,
            @RequestParam String month,
            @RequestBody List<Map<String, Object>> receipts
    ) {
        return monthlyService.getWeeklySummary(userId, month, receipts);
    }
}
