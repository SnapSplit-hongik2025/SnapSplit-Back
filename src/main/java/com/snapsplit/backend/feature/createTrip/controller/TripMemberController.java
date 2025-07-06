package com.snapsplit.backend.feature.createTrip.controller;

import com.snapsplit.backend.feature.auth.dto.KakaoUserResponse;
import com.snapsplit.backend.feature.createTrip.dto.TripMemberResponse;
import com.snapsplit.backend.feature.createTrip.service.TripMemberService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class TripMemberController {

    private final TripMemberService tripMemberService;

    // 유저 코드로 유저 검색
    @Operation(summary = "사용자 검색", description = "유저 코드로 여행에 초대할 사용자를 검색합니다.")
    @GetMapping("/code/{userCode}")
    public ResponseEntity<ApiResponse<TripMemberResponse>> getUserByUserCode(
            @PathVariable String userCode,
            @AuthenticationPrincipal(expression = "id") Long currentUserId
    ) {
        TripMemberResponse response = tripMemberService.findByUserCode(userCode, currentUserId);

        // 검색 결과가 존재하지 않아 비어있을 경우
        if (response == null) {
            return ResponseEntity.ok(ApiResponse.success("검색된 사용자가 없습니다.", null));
        }

        // 검색 결과가 존재할 경우
        return ResponseEntity.ok(ApiResponse.success("사용자 조회 성공", response));
    }
}
