package com.snapsplit.backend.domain.auth.controller;
import com.snapsplit.backend.domain.auth.dto.KakaoTokenResponse;
import com.snapsplit.backend.domain.auth.dto.KakaoUserResponse;
import com.snapsplit.backend.domain.auth.dto.RefreshRequest;
import com.snapsplit.backend.domain.auth.dto.TokenResponse;
import com.snapsplit.backend.domain.auth.service.KakaoOAuthService;
import com.snapsplit.backend.domain.auth.token.RefreshToken;
import com.snapsplit.backend.domain.auth.token.RefreshTokenService;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.global.jwt.JwtUtil;
import com.snapsplit.backend.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class KakaoAuthController {

    private final KakaoOAuthService kakaoOAuthService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/kakao/login")
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

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshAccessToken(@RequestBody RefreshRequest request) {
        // 클라이언트가 보낸 JSON에서 refreshToken 값 추출
        String refreshToken = request.getRefreshToken();

        Optional<RefreshToken> tokenOptional = refreshTokenService.findByToken(refreshToken);
        //해당 refreshToken이 DB에 없는 경우
        if (tokenOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(401, "refresh token이 존재하지 않습니다."));
        }

        RefreshToken refreshTokenEntity = tokenOptional.get();

        //refreshToken 만료 여부 확인
        if (refreshTokenEntity.isExpired()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(401, "refresh token이 만료되었습니다."));
        }

        User user = refreshTokenEntity.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user);

        TokenResponse tokenResponse = new TokenResponse(newAccessToken, refreshToken);

        return ResponseEntity.ok(ApiResponse.success("access token 재발급 성공", tokenResponse));
    }

}