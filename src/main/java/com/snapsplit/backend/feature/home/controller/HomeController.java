package com.snapsplit.backend.feature.home.controller;

import com.snapsplit.backend.feature.home.dto.HomeResponse;
import com.snapsplit.backend.feature.home.service.HomeService;
import com.snapsplit.backend.global.response.ApiResponse;
import com.snapsplit.backend.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "홈", description = "기본 홈/코드로 여행 참여/다가오는 여행/지난 여행")
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "기본 홈 정보 조회", description = "로그인된 사용자 정보를 기반으로 기본 홈에 필요한 정보를 불러옵니다.")
    @GetMapping
    public ApiResponse<HomeResponse> getHome(@AuthenticationPrincipal CustomUserPrincipal user) {
        HomeResponse response = homeService.getHome(user.getId());
        return ApiResponse.success("메인 홈 불러오기 성공", response);
    }
}
