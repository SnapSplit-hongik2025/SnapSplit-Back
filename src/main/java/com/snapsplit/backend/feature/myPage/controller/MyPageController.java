package com.snapsplit.backend.feature.myPage.controller;

import com.snapsplit.backend.domain.user.repository.UserRepository;
import com.snapsplit.backend.feature.myPage.dto.MyPageUpdateRequest;
import com.snapsplit.backend.feature.myPage.dto.MyPageResponse;
import com.snapsplit.backend.feature.myPage.service.MyPageService;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.global.response.ApiResponse;
import com.snapsplit.backend.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/home/myPage")
public class MyPageController {

    private final MyPageService myPageService;
    private final UserRepository userRepository;

    // GET /home/myPage
    // 마이페이지 조회
    @Operation(summary = "마이페이지 조회", description = "마이페이지 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        Long userId = principal.getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        MyPageResponse response = myPageService.getMyPage(user);
        return ResponseEntity.ok(ApiResponse.success("마이페이지 조회 성공", response));
    }

    // PUT /home/myPage
    // 마이페이지 수정
    @Operation(summary = "마이페이지 수정", description = "기존 마이페이지 정보를 수정합니다.")
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody MyPageUpdateRequest request
    ) {
        Long userId = principal.getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        myPageService.updateProfile(user, request.name(), request.profileImageUrl());
        return ResponseEntity.ok(ApiResponse.success("프로필 수정 완료", null));
    }
}
