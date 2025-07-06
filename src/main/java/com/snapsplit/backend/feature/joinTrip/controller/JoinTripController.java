package com.snapsplit.backend.feature.joinTrip.controller;

import com.snapsplit.backend.feature.joinTrip.dto.JoinTripRequest;
import com.snapsplit.backend.feature.joinTrip.dto.JoinTripResponse;
import com.snapsplit.backend.feature.joinTrip.dto.JoinTripResult;
import com.snapsplit.backend.feature.joinTrip.service.JoinTripService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class JoinTripController {

    private final JoinTripService joinTripService;

    @Operation(summary = "초대 코드로 여행 참여", description = "참여하고 싶은 초대 코드를 입력하여 여행에 참여합니다.")
    @PostMapping("/join")
    public ApiResponse<JoinTripResponse> joinTrip(@RequestBody JoinTripRequest request) {
        JoinTripResult result = joinTripService.joinTrip(request.getUserId(), request.getInviteCode());

        // 실패했을 시
        if (!result.isSuccess()) {
            return ApiResponse.fail(400, result.getMessage());
        }

        // 성공했을 시
        return ApiResponse.success(result.getMessage(), result.getData());
    }
}