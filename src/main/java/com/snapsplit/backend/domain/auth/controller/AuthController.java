package com.snapsplit.backend.domain.auth.controller;

import com.snapsplit.backend.domain.auth.repository.RefreshTokenRepository;
import com.snapsplit.backend.domain.auth.service.TokenBlacklistService;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import com.snapsplit.backend.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

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

        // 유효성 확인
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰입니다.");
        }

        // 토큰 타입 확인 -> refresh로 이중로그아웃 방지
        String tokenType = jwtUtil.getTokenType(token);
        if (!"access".equals(tokenType)) {
            return ResponseEntity.badRequest().body("Access token으로만 로그아웃 가능합니다.");
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

}
