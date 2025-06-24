package com.snapsplit.backend.domain.auth.controller;

import com.snapsplit.backend.domain.auth.repository.RefreshTokenRepository;
import com.snapsplit.backend.domain.auth.service.TokenBlacklistService;
import com.snapsplit.backend.domain.auth.token.RefreshToken;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import com.snapsplit.backend.global.jwt.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<String> logout(@CookieValue(value = "accessToken", required = false) String accessToken,
                                         @CookieValue(value = "refreshToken", required = false) String refreshToken,
                                         HttpServletResponse response) {
        // access token이 없으면 바로 반환
        if (accessToken == null) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("access token이 존재하지 않습니다.");
        }

        String tokenType;
        try {
            tokenType = jwtUtil.getTokenType(accessToken);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("유효하지 않은 access token입니다.");
        }

        if (!"access".equals(tokenType)) {
            return ResponseEntity.badRequest().body("Access token으로만 로그아웃 가능합니다.");
        }

        if (!jwtUtil.validateToken(accessToken)) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰입니다.");
        }

        // 사용자 정보 조회 후 refreshToken 삭제
        String kakaoId = jwtUtil.getKakaoIdFromToken(accessToken);
        User user = userRepository.findByKakaoId(kakaoId).orElseThrow();
        refreshTokenRepository.deleteByUser(user);

        // accessToken 블랙리스트 등록
        Date expiration = jwtUtil.getExpiration(accessToken);
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        tokenBlacklistService.blacklistToken(accessToken, remainingMillis);

        // 쿠키 삭제
        Cookie deleteAccess = new Cookie("accessToken", null);
        deleteAccess.setMaxAge(0);
        deleteAccess.setPath("/");

        Cookie deleteRefresh = new Cookie("refreshToken", null);
        deleteRefresh.setMaxAge(0);
        deleteRefresh.setPath("/");

        response.addCookie(deleteAccess);
        response.addCookie(deleteRefresh);

        return ResponseEntity.ok("로그아웃 완료!");
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestHeader("Authorization") String refreshTokenHeader,
                                                HttpServletResponse response) {
        String refreshToken = refreshTokenHeader;
        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }

        String tokenType;
        try {
            tokenType = jwtUtil.getTokenType(refreshToken);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST)
                    .body("유효하지 않은 토큰 형식입니다.");
        }

        if (!"refresh".equals(jwtUtil.getTokenType(refreshToken))) {
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST)
                    .body("refresh token만 허용됩니다.");
        }

        if (!jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("유효하지 않은 refresh token입니다.");
        }

        String kakaoId = jwtUtil.getKakaoIdFromToken(refreshToken);
        User user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        RefreshToken storedToken = refreshTokenRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("저장된 refresh token이 없습니다."));

        if (!storedToken.getToken().equals(refreshToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("저장된 refresh token과 일치하지 않습니다.");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user);

        // Set access token in cookie
        Cookie cookie = new Cookie("accessToken", newAccessToken);
        cookie.setHttpOnly(true); // JavaScript에서 접근 불가
        cookie.setPath("/");      // 전체 경로에 대해 유효
        cookie.setMaxAge(60 * 10); // 10분
        response.addCookie(cookie);

        return ResponseEntity.ok().body(Map.of("message", "access token이 쿠키에 저장되었습니다."));
    }

}
