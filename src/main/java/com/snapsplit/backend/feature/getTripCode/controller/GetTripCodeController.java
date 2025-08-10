package com.snapsplit.backend.feature.getTripCode.controller;

import com.snapsplit.backend.feature.getTripCode.dto.GetTripCodeResponse;
import com.snapsplit.backend.feature.getTripCode.service.GetTripCodeService;
import com.snapsplit.backend.feature.settlement.dto.SettlementCreationResponse;
import com.snapsplit.backend.feature.settlement.dto.SettlementRequest;
import com.snapsplit.backend.global.aop.CheckTripMember;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "여행 코드", description = "여행 코드 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/trips")
public class GetTripCodeController {

    private final GetTripCodeService getTripCodeService;

    @Operation(summary = "여행 코드 조회", description = "여행 ID를 기반으로 여행 코드를 조회합니다.")
    @GetMapping("/{tripId}/tripcode")
    @CheckTripMember
    public ApiResponse<GetTripCodeResponse> createSettlement(@PathVariable Long tripId) {
        String tripCode = getTripCodeService.getTripCode(tripId);

        return ApiResponse.success(
                "여행 코드 조회 성공", new GetTripCodeResponse(tripCode)
        );
    }
}
