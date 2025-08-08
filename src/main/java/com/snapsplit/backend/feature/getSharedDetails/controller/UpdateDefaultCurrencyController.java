package com.snapsplit.backend.feature.getSharedDetails.controller;

import com.snapsplit.backend.feature.getSharedDetails.dto.UpdateDefaultCurrencyResponse;
import com.snapsplit.backend.feature.getSharedDetails.service.UpdateDefaultCurrencyService;
import com.snapsplit.backend.global.aop.CheckTripMember;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "공동경비", description = "공동경비 조회/넣기/빼기/대표 통화 변경")
@RestController
@RequestMapping("/trips/{tripId}/budget")
@RequiredArgsConstructor
public class UpdateDefaultCurrencyController {

    private final UpdateDefaultCurrencyService updateDefaultCurrencyService;

    @Operation(summary = "대표 통화 변경", description = "대표 통화를 변경합니다.")
    @PatchMapping
    @CheckTripMember
    public ResponseEntity<ApiResponse<UpdateDefaultCurrencyResponse>> updateDefaultCurrency(
            @PathVariable Long tripId,
            @RequestParam String newDefaultCur
    ) {
        UpdateDefaultCurrencyResponse response = updateDefaultCurrencyService.updateDefaultCurrency(tripId, newDefaultCur);
        return ResponseEntity.ok(ApiResponse.success("대표 통화 변경 성공", response));
    }

}
