package com.snapsplit.backend.feature.auth.service;

import com.snapsplit.backend.feature.auth.dto.KakaoTokenResponse;
import com.snapsplit.backend.feature.auth.dto.KakaoUserResponse;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kakao.login.client-id}")
    private String clientId;

    @Value("${kakao.login.client-secret}")
    private String clientSecret;

    @Value("${kakao.login.redirect-uri}")
    private String redirectUri;

    public KakaoTokenResponse getToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
                    "https://kauth.kakao.com/oauth/token",
                    request,
                    KakaoTokenResponse.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("만료되었거나 유효하지 않은 인가코드입니다. 다시 로그인해주세요.");
        }
    }

    public KakaoUserResponse getUserInfo(String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<KakaoUserResponse> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                request,
                KakaoUserResponse.class
        );

        KakaoUserResponse user = response.getBody();

        return user;
    }

    public User getOrRegisterUser(KakaoUserResponse kakaoUser) {
        String kakaoId = kakaoUser.getId().toString();
        String nickname = kakaoUser.getProperties().getNickname();
        String profileImage = kakaoUser.getProperties().getProfile_image();


        return userRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .kakaoId(kakaoId)
                            .name(nickname)
                            .profileImage(profileImage)
                            .userCode(generateUserCode())
                            .build();
                    return userRepository.save(newUser);
                });
    }

    private String generateUserCode() {
        return new Random().ints(48, 122 + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(6)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
