package com.snapsplit.backend.feature.addExpense.controller;

import com.snapsplit.backend.feature.addExpense.dto.AddExpenseRequest;
import com.snapsplit.backend.feature.addExpense.service.AddExpenseService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityNotFoundException;

import java.util.Map;

@RestController
@RequestMapping("/trips/{tripId}/expense")
@RequiredArgsConstructor
public class AddExpenseController {

    private final AddExpenseService addExpenseService;

    //지출 추가
    @PostMapping
    @Operation(
            summary = "지출 추가",
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
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(
                    ApiResponse.fail(404, e.getMessage())
            );
        }
    }


    //지출 삭제
    @DeleteMapping("/{expenseId}")
    @Operation(
            summary = "지출 삭제",
            description = "특정 여행(tripId) 내 특정 지출(expenseId)을 삭제합니다. 관련된 Pay와 Split 내역도 함께 삭제됩니다."
    )
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable Long tripId,
            @PathVariable Long expenseId
    ) {
        try {
            addExpenseService.deleteExpense(tripId, expenseId);
            return ResponseEntity.ok(
                    ApiResponse.success("지출이 성공적으로 삭제되었습니다.", null)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(400, e.getMessage())
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(
                    ApiResponse.fail(404, e.getMessage())
            );
        }
    }


    //지출 수정
    @PutMapping("/{expenseId}")
    @Operation(
            summary = "지출 수정",
            description = "기존 지출 내역을 삭제하고, 새로운 지출 정보로 교체합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Long>>> updateExpense(
            @PathVariable Long tripId,
            @PathVariable Long expenseId,
            @RequestBody AddExpenseRequest request
    ) {
        try {
            Long newExpenseId = addExpenseService.updateExpense(tripId, expenseId, request);
            Map<String, Long> response = Map.of("expenseId", newExpenseId);

            return ResponseEntity.ok(
                    ApiResponse.success("지출이 성공적으로 수정되었습니다.", response)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(400, e.getMessage())
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(
                    ApiResponse.fail(404, e.getMessage())
            );
        }
    }



}
