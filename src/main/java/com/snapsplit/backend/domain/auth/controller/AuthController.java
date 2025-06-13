package com.snapsplit.backend.domain.auth.controller;

import com.snapsplit.backend.domain.auth.repository.RefreshTokenRepository;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import com.snapsplit.backend.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String accessToken) {
        String kakaoId = jwtUtil.getKakaoIdFromToken(accessToken); // 토큰에서 사용자 정보 추출
        User user = userRepository.findByKakaoId(kakaoId).orElseThrow();

        refreshTokenRepository.deleteByUser(user); // DB에 저장된 RefreshToken 제거

        return ResponseEntity.ok("로그아웃 완료!");
    }
}
