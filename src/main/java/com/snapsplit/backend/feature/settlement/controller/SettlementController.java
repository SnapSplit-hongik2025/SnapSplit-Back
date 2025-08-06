package com.snapsplit.backend.feature.settlement.controller;

import com.snapsplit.backend.feature.settlement.dto.SettlementCreationResponse;
import com.snapsplit.backend.feature.settlement.dto.SettlementRequest;
import com.snapsplit.backend.feature.settlement.service.SettlementDetailService;
import com.snapsplit.backend.feature.settlement.service.SettlementService;
import com.snapsplit.backend.global.aop.CheckTripMember;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trips")
public class SettlementController {

    private final SettlementService settlementService;
    private final SettlementDetailService settlementDetailService;

    @Operation(summary = "정산하기", description = "정산 시작일 및 종료일을 기반으로 정산을 처리합니다.")
    @PostMapping("/{tripId}/settlements")
    @CheckTripMember
    public ApiResponse<SettlementCreationResponse> createSettlement(@PathVariable Long tripId,
                                                                    @RequestBody SettlementRequest request) {
        Long settlementId = settlementService.createSettlement(tripId, request);

        return ApiResponse.success(
                "정산이 완료되었습니다.",
                new SettlementCreationResponse(settlementId)
        );
    }

    @Operation(summary = "정산 영수증 상세 조회", description = "정산 ID를 기반으로 정산 상세 내역을 조회합니다.")
    @GetMapping("/{tripId}/settlement")
    @CheckTripMember
    public ApiResponse<?> getSettlementDetails(@PathVariable Long tripId,
                                               @RequestParam Long settlementId) {
        return ApiResponse.success(
                "정산 상세 내역 조회 성공",
                settlementDetailService.getSettlementDetails(tripId, settlementId)
        );
    }

}
