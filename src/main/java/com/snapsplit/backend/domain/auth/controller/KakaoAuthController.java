package com.snapsplit.backend.domain.auth.controller;

import com.snapsplit.backend.domain.auth.dto.KakaoTokenResponse;
import com.snapsplit.backend.domain.auth.dto.KakaoUserResponse;
import com.snapsplit.backend.domain.auth.dto.TokenResponse;
import com.snapsplit.backend.domain.auth.service.KakaoOAuthService;
import com.snapsplit.backend.domain.auth.token.RefreshTokenService;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.global.jwt.JwtUtil;
import com.snapsplit.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/kakao")
public class KakaoAuthController {

    private final KakaoOAuthService kakaoOAuthService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> kakaoLogin(@RequestParam String code) {
        // 인가 코드로 카카오 access token 받기
        KakaoTokenResponse tokenResponse = kakaoOAuthService.getToken(code);

        // 카카오 access token으로 사용자 정보 요청
        KakaoUserResponse kakaoUser = kakaoOAuthService.getUserInfo(tokenResponse.getAccessToken());

        // 기존 회원인지 확인하고 없으면 등록
        User user = kakaoOAuthService.getOrRegisterUser(kakaoUser);

        // 자체 access/refresh JWT 발급
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        refreshTokenService.save(user, refreshToken, jwtUtil.getRefreshTokenExpiry());

        // access token 및 refresh token dto
        TokenResponse jwtResponse = new TokenResponse(accessToken, refreshToken);

        return ResponseEntity.ok(
                ApiResponse.success("카카오 로그인 성공", jwtResponse)
        );
    }
}
