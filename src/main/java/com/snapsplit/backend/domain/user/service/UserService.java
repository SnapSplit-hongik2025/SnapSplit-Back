package com.snapsplit.backend.domain.user.service;

import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findOrCreateUser(String kakaoId, OAuth2User user) {
        return userRepository.findByKakaoId(kakaoId).orElseGet(() -> {
            String nickname = ((Map<String, Object>) ((Map<String, Object>) user.getAttributes()
                    .get("kakao_account")).get("profile")).get("nickname").toString();

            String profileImage = ((Map<String, Object>) ((Map<String, Object>) user.getAttributes()
                    .get("kakao_account")).get("profile")).get("profile_image_url").toString();

            String userCode = UUID.randomUUID().toString();

            User newUser = User.builder()
                    .kakaoId(kakaoId)
                    .name(nickname)
                    .profileImage(profileImage)
                    .userCode(userCode)
                    .build();

            return userRepository.save(newUser);
        });
    }
}

