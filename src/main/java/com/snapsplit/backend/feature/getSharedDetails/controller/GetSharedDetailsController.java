package com.snapsplit.backend.feature.getSharedDetails.controller;

import com.snapsplit.backend.feature.getSharedDetails.dto.SharedDetailsResponse;
import com.snapsplit.backend.feature.getSharedDetails.service.GetSharedDetailsService;
import com.snapsplit.backend.global.aop.CheckTripMember;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trips/{tripId}/budget/details")
@RequiredArgsConstructor
public class GetSharedDetailsController {

    private final GetSharedDetailsService getSharedDetailsService;

    @Operation(summary = "공동경비 세부내역 조회", description = "공동경비로 발생한 지출 및 입출금 내역을 조회합니다.")
    @GetMapping
    @CheckTripMember
    public ResponseEntity<ApiResponse<SharedDetailsResponse>> getSharedDetails(
            @PathVariable Long tripId
    ) {
        SharedDetailsResponse response = getSharedDetailsService.getSharedDetails(tripId);
        return ResponseEntity.ok(ApiResponse.success("공동경비 세부내역 조회 성공", response));
    }

}
