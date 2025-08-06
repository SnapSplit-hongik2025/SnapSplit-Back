package com.snapsplit.backend.feature.settlement.controller;

import com.snapsplit.backend.feature.settlement.dto.SettlementPageResponse;
import com.snapsplit.backend.feature.settlement.service.SettlementPageService;
import com.snapsplit.backend.global.aop.CheckTripMember;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trips")
public class SettlementPageController {

    private final SettlementPageService settlementPageService;

    @Operation(summary = "정산 영수증 시작 페이지 조회", description = "이미 정산완료된 정산 영수증 내역 날짜 정보와 여행 시작일 및 종료일을 제공합니다.")
    @GetMapping("/{tripId}/settlements")
    @CheckTripMember
    public ApiResponse<SettlementPageResponse> getSettlementPage(@PathVariable Long tripId) {
        SettlementPageResponse response = settlementPageService.getSettlementPage(tripId);
        return ApiResponse.success("정산 페이지 조회 성공", response);
    }
}
