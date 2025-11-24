package com.snapsplit.backend.feature.auth.controller;
import com.snapsplit.backend.feature.auth.dto.*;
import com.snapsplit.backend.feature.auth.service.KakaoOAuthService;
import com.snapsplit.backend.feature.auth.token.RefreshToken;
import com.snapsplit.backend.feature.auth.token.RefreshTokenService;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.global.jwt.JwtUtil;
import jakarta.servlet.http.Cookie;
import com.snapsplit.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@Tag(name = "로그인/로그아웃", description = "로그인/로그아웃/토큰 재발급")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class KakaoAuthController {

    private final KakaoOAuthService kakaoOAuthService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.access.expiration-seconds}")
    private long accessTokenValidityInSeconds;

    @Value("${jwt.refresh.expiration-seconds}")
    private long refreshTokenValidityInSeconds;

    @Operation(summary = "카카오 로그인",
            description = "전달받은 인가 코드로 카카오에서 사용자 정보를 받고, " +
                    "미가입된 회원일 시 회원가입 후 로그인하고 " +
                    "가입된 회원일 시 바로 로그인합니다.")
    @PostMapping("/kakao/login")
    public ResponseEntity<ApiResponse<LoginResponse>> kakaoLogin(@RequestParam String code) {
        try {
            // 인가 코드로 카카오 access token 받기
            KakaoTokenResponse tokenResponse = kakaoOAuthService.getToken(code);

            // 카카오 access token으로 사용자 정보 요청
            KakaoUserResponse kakaoUser = kakaoOAuthService.getUserInfo(tokenResponse.getAccessToken());

            // 기존 회원인지 확인하고 없으면 등록
            User user = kakaoOAuthService.getOrRegisterUser(kakaoUser);

            // 자체 access/refresh JWT 발급
            String accessToken = jwtUtil.generateAccessToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user);

            // refresh token DB에 저장
            refreshTokenService.save(user, refreshToken);

            // 토큰을 담을 HttpOnly 쿠키 생성
            ResponseCookie accessTokenCookie = createCookie("accessToken", accessToken, accessTokenValidityInSeconds);
            ResponseCookie refreshTokenCookie = createCookie("refreshToken", refreshToken, refreshTokenValidityInSeconds);

            // 유저 정보를 담은 응답 dto
            LoginResponse response = LoginResponse.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .profileImage(user.getProfileImage())
                    .userCode(user.getUserCode())
                    .build();

            // 헤더에 쿠키 담아서 응답
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(ApiResponse.success("카카오 로그인 성공", response));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(HttpStatus.UNAUTHORIZED.value(), e.getMessage()));
        }
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/kakao/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        System.out.println("[로그아웃] 컨트롤러 진입 성공");
        // 1. 쿠키에서 token 추출
        String accessToken = getCookieValue(request, "accessToken");
        String refreshToken = getCookieValue(request, "refreshToken");

        // 2. access token 블랙리스트 등록 (redis 사용)
        if (accessToken != null) {
            long expiration = jwtUtil.getExpiration(accessToken).getTime() - System.currentTimeMillis();
            redisTemplate.opsForValue().set(accessToken, "logout", expiration, TimeUnit.MILLISECONDS);
        }

        // 3. db에서 refresh token 삭제
        if (refreshToken != null) {
            refreshTokenService.deleteByToken(refreshToken);
        }

        // 4. 클라이언트의 쿠키 삭제
        ResponseCookie expiredAccessTokenCookie = createCookie("accessToken", "", 0);
        ResponseCookie expiredRefreshTokenCookie = createCookie("refreshToken", "", 0);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredAccessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, expiredRefreshTokenCookie.toString())
                .body(ApiResponse.success("로그아웃 완료", null));
    }

    @Operation(summary = "access 토큰 재발급", description = "리프레시 토큰을 기반으로 액세스 토큰을 재발급합니다.")
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshAccessToken(@CookieValue(name = "refreshToken", required = false) String refreshToken) {

        if (refreshToken == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail(401, "Refresh token이 쿠키에 존재하지 않습니다."));
        }

        // 1. DB에서 Refresh Token 조회 및 유효성 검증
        RefreshToken refreshTokenEntity = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found."));

        if (refreshTokenEntity.isExpired()) {
            refreshTokenService.deleteByToken(refreshToken); // 만료된 토큰은 DB에서 삭제
            throw new IllegalArgumentException("Refresh token has expired.");
        }

        // 2. 새로운 Access, Refresh 토큰 생성 (보안 강화를 위해 Refresh Token도 재발급 - Token Rotation)
        User user = refreshTokenEntity.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        // 3. 기존 Refresh Token을 새로운 토큰으로 DB에 업데이트
        refreshTokenService.save(user, newRefreshToken);

        // 4. 새로운 토큰들을 쿠키에 담아 응답
        ResponseCookie accessTokenCookie = createCookie("accessToken", newAccessToken, accessTokenValidityInSeconds);
        ResponseCookie refreshTokenCookie = createCookie("refreshToken", newRefreshToken, refreshTokenValidityInSeconds);

        return ResponseEntity.noContent() // 204 No Content: 바디 없이 헤더만 전달
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .build();
    }

    private ResponseCookie createCookie(String name, String value, long maxAgeInSeconds) {
        return ResponseCookie.from(name, value)
                .domain("snap-split.co.kr")
                .httpOnly(true)
                .secure(true)       // HTTPS 환경에서만 전송
                .path("/")          // 쿠키가 전송될 경로
                .maxAge(maxAgeInSeconds) // 쿠키 유효 기간
                .sameSite("None")
                .build();
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }


}
