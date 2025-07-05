package com.snapsplit.backend.domain.auth.dto;

import lombok.Data;

@Data
public class KakaoUserResponse {
    private Long id;
    private KakaoProperties properties;
    private KakaoAccount kakao_account;

    @Data
    public static class KakaoProperties {
        private String nickname;
        private String profile_image;
    }

    @Data
    public static class KakaoAccount {
        private String email;
    }
}
