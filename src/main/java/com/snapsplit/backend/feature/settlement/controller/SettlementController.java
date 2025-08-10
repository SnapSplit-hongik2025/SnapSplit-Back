package com.snapsplit.backend.feature.settlement.controller;

import com.snapsplit.backend.feature.settlement.dto.SettlementCreationResponse;
import com.snapsplit.backend.feature.settlement.dto.SettlementDetailResponse;
import com.snapsplit.backend.feature.settlement.dto.SettlementExpenseResponse;
import com.snapsplit.backend.feature.settlement.dto.SettlementRequest;
import com.snapsplit.backend.feature.settlement.service.SettlementDetailService;
import com.snapsplit.backend.feature.settlement.service.SettlementExpenseService;
import com.snapsplit.backend.feature.settlement.service.SettlementService;
import com.snapsplit.backend.global.aop.CheckTripMember;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "정산하기", description = "정산하기/정산 영수증/정산 영수증 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/trips")
public class SettlementController {

    private final SettlementService settlementService;
    private final SettlementDetailService settlementDetailService;
    private final SettlementExpenseService settlementExpenseService;

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
    public ApiResponse<SettlementDetailResponse> getSettlementDetails(@PathVariable Long tripId,
                                                                      @RequestParam Long settlementId) {
        return ApiResponse.success(
                "정산 상세 내역 조회 성공",
                settlementDetailService.getSettlementDetails(tripId, settlementId)
        );
    }

    @Operation(summary = "정산 영수증 개별 지출 상세 금액 조회", description = "여행 멤버 ID를 기반으로 정산 내역 상세 조회에서 개별 지출에 대한 세부 정보를 조회합니다.")
    @GetMapping(value = "/{tripId}/settlement/expenses")
    @CheckTripMember
    public ApiResponse<SettlementExpenseResponse> getSettlementExpense(@PathVariable Long tripId,
                                                                       @RequestParam Long settlementId,
                                                                       @RequestParam Long memberId) {
        return ApiResponse.success(
                "정산 영수증 개별 지출 상세 금액 조회 성공",
                settlementExpenseService.getSettlementExpense(tripId, settlementId, memberId)
        );
    }

}
