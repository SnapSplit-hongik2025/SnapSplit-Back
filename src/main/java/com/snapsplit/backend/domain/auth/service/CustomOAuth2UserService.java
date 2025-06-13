package com.snapsplit.backend.domain.auth.service;

import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {

        // 카카오 API 요청으로 사용자 정보 받아옴
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(request);
        Map<String, Object> kakaoAttributes = oAuth2User.getAttributes();

        // id랑 properties 추출
        String kakaoId = oAuth2User.getAttribute("id").toString();
        Map<String, Object> properties = oAuth2User.getAttribute("properties");

        // properties에서 nickname과 profile_image 추출
        String nickname = (String) properties.get("nickname");
        String profileImage = (String) properties.get("profile_image");

        // 유저코드는 예시로 UUID 사용
        String userCode = UUID.randomUUID().toString();

        // 회원가입 처리 로직
        User user = userRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .kakaoId(kakaoId)
                        .name(nickname)
                        .profileImage(profileImage)
                        .userCode(userCode)
                        .build()));

        // 인증 성공 후 JWT 생성을 위한 사용자 정보의 원천이 되는 객체
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                kakaoAttributes,
                "id"
        );
    }
}
