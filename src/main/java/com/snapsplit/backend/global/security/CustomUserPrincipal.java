package com.snapsplit.backend.global.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomUserPrincipal {
    // 커스텀 인증 객체
    private Long id; // 사용자 아이디
    private String kakaoId; // 카카오 아이디
    private String nickname; // 닉네임
}
