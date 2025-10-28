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
    @Operation(summary = "달별 탄소 절감량 계산")
    public Map<String, Object> getMonthlySummary(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        String month = (String) body.get("month");
        List<Map<String, Object>> receipts = (List<Map<String, Object>>) body.get("receipts");

        return monthlyService.getMonthlySummary(userId, month, receipts);
    }

    @PostMapping("/product")
    @Operation(summary = "상품별 탄소 절감량 계산")
    public List<Map<String, Object>> getMonthlyDetail(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        String month = (String) body.get("month");
        List<Map<String, Object>> receipts = (List<Map<String, Object>>) body.get("receipts");

        return monthlyService.getMonthlyDetail(userId, month, receipts);
    }

    @PostMapping("/weekly")
    @Operation(summary = "주별 탄소 절감량 계산")
    public List<Map<String, Object>> getWeeklySummary(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        String month = (String) body.get("month");
        List<Map<String, Object>> receipts = (List<Map<String, Object>>) body.get("receipts");

        return monthlyService.getWeeklySummary(userId, month, receipts);
    }
}
