package com.snapsplit.backend.feature.myPage.service;

import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.feature.myPage.dto.MyFaceResponse;
import com.snapsplit.backend.feature.myPage.dto.MyPageResponse;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;

    public MyPageResponse getMyPage(User user) {
        return new MyPageResponse(
                user.getName(),
                user.getProfileImage(),
                user.getUserCode()
        );
    }

    public void updateProfile(User user, String name, String profileImageUrl) {
        if (name != null && !name.isBlank()) {
            user.setName(name); //이름(닉네임) 변경
        }

        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            user.setProfileImage(profileImageUrl); //프로필 이미지 변경
        }

        userRepository.save(user); // 변경 감지 또는 save
    }

    @Transactional(readOnly = true)
    public MyFaceResponse getMyFaceInfo(User user) {
        String faceImageUrl = user.getFaceImageUrl();
        boolean isRegistered = (faceImageUrl != null && !faceImageUrl.isEmpty());

        return MyFaceResponse.builder()
                .registered(isRegistered)
                .faceImageUrl(faceImageUrl)
                .build();
    }
}
