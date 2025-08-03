package com.snapsplit.backend.feature.getCategoryExpense.controller;

import com.snapsplit.backend.feature.getCategoryExpense.dto.CategoryExpenseResponse;
import com.snapsplit.backend.feature.getCategoryExpense.service.CategoryExpenseService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trips/{tripId}/statistics")
@RequiredArgsConstructor
public class CategoryExpenseController {

    private final CategoryExpenseService categoryExpenseService;

    @GetMapping
    @Operation(summary = "카테고리별 누적 지출 조회", description = "여행별로 7가지 카테고리 기준 누적 지출 금액(KRW)을 반환합니다.")
    public ApiResponse<CategoryExpenseResponse> getCategoryStatistics(@PathVariable Long tripId) {
        CategoryExpenseResponse response = categoryExpenseService.getCategoryStatistics(tripId);
        return ApiResponse.success("카테고리별 누적 지출 조회 성공", response);
    }
}
