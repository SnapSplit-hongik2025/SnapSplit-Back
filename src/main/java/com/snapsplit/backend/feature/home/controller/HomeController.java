package com.snapsplit.backend.feature.home.controller;

import com.snapsplit.backend.feature.home.dto.HomeRequest;
import com.snapsplit.backend.feature.home.dto.HomeResponse;
import com.snapsplit.backend.feature.home.service.HomeService;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "기본 홈 조회", description = "사용자 ID를 기반으로 기본 홈에 필요한 정보를 불러옵니다.")
    @PostMapping
    public ApiResponse<HomeResponse> getHome(@RequestBody HomeRequest request) {
        HomeResponse response = homeService.getHome(request.getUserId());
        return ApiResponse.success("메인 홈 불러오기 성공", response);
    }
}
