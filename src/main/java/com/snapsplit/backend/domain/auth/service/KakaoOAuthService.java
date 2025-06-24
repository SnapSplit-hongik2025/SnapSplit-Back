package com.snapsplit.backend.domain.auth.service;

import com.snapsplit.backend.domain.auth.dto.KakaoTokenResponse;
import com.snapsplit.backend.domain.auth.dto.KakaoUserResponse;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

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
        System.out.println("[카카오 인가코드 수신] code = " + code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
                "https://kauth.kakao.com/oauth/token",
                request,
                KakaoTokenResponse.class
        );

        System.out.println("[카카오 토큰 응답 수신] access_token = " + response.getBody().getAccessToken());
        return response.getBody();
    }

    public KakaoUserResponse getUserInfo(String accessToken) {
        System.out.println("[카카오 유저 정보 요청] accessToken = " + accessToken);

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
        System.out.println("[카카오 유저 정보 수신] id = " + user.getId() + ", nickname = " + user.getProperties().getNickname());

        return user;
    }

    public User getOrRegisterUser(KakaoUserResponse kakaoUser) {
        String kakaoId = kakaoUser.getId().toString();
        String nickname = kakaoUser.getProperties().getNickname();
        String profileImage = kakaoUser.getProperties().getProfile_image();

        System.out.println("[DB 조회 시도] kakaoId = " + kakaoId);

        return userRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> {
                    System.out.println("[신규 회원 등록] nickname = " + nickname);
                    User newUser = User.builder()
                            .kakaoId(kakaoId)
                            .name(nickname)
                            .profileImage(profileImage)
                            .userCode(UUID.randomUUID().toString())
                            .build();
                    return userRepository.save(newUser);
                });
    }

}
