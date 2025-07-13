package com.snapsplit.backend.feature.addExpense.controller;

import com.snapsplit.backend.feature.addExpense.dto.AddExpenseRequest;
import com.snapsplit.backend.feature.addExpense.service.AddExpenseService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/trips/{tripId}/expense")
@RequiredArgsConstructor
public class AddExpenseController {

    private final AddExpenseService addExpenseService;

    @PostMapping
    @Operation(
            summary = "여행 지출 추가",
            description = "여행 중 발생한 개별 지출 내역을 등록합니다. 결제자와 분담자 정보를 함께 포함해야 합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Long>>> addExpense(
            @PathVariable Long tripId,
            @RequestBody AddExpenseRequest request
    ){
        try {
            Long expenseId = addExpenseService.addExpense(tripId, request);
            Map<String, Long> response = Map.of("expenseId", expenseId);
            return ResponseEntity.ok(
                    ApiResponse.success("지출이 성공적으로 등록되었습니다.", response)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(400, e.getMessage())
            );
        }
    }
}
