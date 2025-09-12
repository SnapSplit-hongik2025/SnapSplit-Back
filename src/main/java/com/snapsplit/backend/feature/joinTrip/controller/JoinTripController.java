package com.snapsplit.backend.feature.joinTrip.controller;

import com.snapsplit.backend.feature.joinTrip.dto.JoinTripRequest;
import com.snapsplit.backend.feature.joinTrip.dto.JoinTripResponse;
import com.snapsplit.backend.feature.joinTrip.service.JoinTripService;
import com.snapsplit.backend.global.response.ApiResponse;
import com.snapsplit.backend.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "홈", description = "기본 홈/코드로 여행 참여/다가오는 여행/지난 여행")
@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class JoinTripController {

    private final JoinTripService joinTripService;

    @Operation(summary = "초대 코드로 여행 참여", description = "참여하고 싶은 초대 코드를 입력하여 여행에 참여합니다.")
    @PostMapping("/join")
    public ApiResponse<JoinTripResponse> joinTrip(@RequestBody JoinTripRequest request, @AuthenticationPrincipal CustomUserPrincipal user) {
        JoinTripResponse response = joinTripService.joinTrip(user.getId(), request.getInviteCode());
        return ApiResponse.success("여행에 성공적으로 참여가 완료되었습니다.", response);
    }
}