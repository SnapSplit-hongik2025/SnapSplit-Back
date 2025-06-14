package com.snapsplit.backend.domain.auth.controller;

import com.snapsplit.backend.domain.auth.repository.RefreshTokenRepository;
import com.snapsplit.backend.domain.auth.service.TokenBlacklistService;
import com.snapsplit.backend.domain.auth.token.RefreshToken;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import com.snapsplit.backend.global.jwt.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String tokenHeader) {
        // "Bearer " 접두사 제거
        String token = tokenHeader;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String tokenType;
        try {
            tokenType = jwtUtil.getTokenType(token);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("유효하지 않은 access token입니다.");
        }

        // 토큰 타입 확인 -> refresh로 이중로그아웃 방지
        if (!"access".equals(tokenType)) {
            return ResponseEntity.badRequest().body("Access token으로만 로그아웃 가능합니다.");
        }

        // 유효성 확인
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰입니다.");
        }


        // 토큰에서 사용자 정보 추출 및DB에 저장된 RefreshToken 제거
        String kakaoId = jwtUtil.getKakaoIdFromToken(token);
        User user = userRepository.findByKakaoId(kakaoId).orElseThrow();
        refreshTokenRepository.deleteByUser(user);

        // AccessToken 블랙리스트 등록
        Date expiration = jwtUtil.getExpiration(token);
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        tokenBlacklistService.blacklistToken(token, remainingMillis);

        return ResponseEntity.ok("로그아웃 완료!");
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestHeader("Authorization") String refreshTokenHeader) {
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

        // tokenType 확인
        if (!"refresh".equals(jwtUtil.getTokenType(refreshToken))) {
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST)
                    .body("refresh token만 허용됩니다.");
        }

        // 토큰 유효성 검사
        if (!jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("유효하지 않은 refresh token입니다.");
        }

        // 사용자 정보 추출
        String kakaoId = jwtUtil.getKakaoIdFromToken(refreshToken);
        User user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // DB에서 refresh token 일치 여부 확인 (보안 강화 목적)
        RefreshToken storedToken = refreshTokenRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("저장된 refresh token이 없습니다."));
        if (!storedToken.getToken().equals(refreshToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body("저장된 refresh token과 일치하지 않습니다.");
        }

        // 새로운 access token 발급
        String newAccessToken = jwtUtil.generateAccessToken(user);

        return ResponseEntity.ok()
                .body("{\"accessToken\": \"" + newAccessToken + "\"}");
    }
}
