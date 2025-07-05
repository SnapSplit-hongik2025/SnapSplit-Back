package com.snapsplit.backend.feature.myPage.service;

import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.feature.myPage.dto.UserMyPageResponse;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;

    public UserMyPageResponse getMyPage(User user) {
        return new UserMyPageResponse(
                user.getName(),
                user.getProfileImage(),
                user.getUserCode()
        );
    }

    public void updateProfile(User user, String name, MultipartFile profileImage) {
        if (name != null && !name.isBlank()) {
            user.setName(name);
        }

        if (profileImage != null && !profileImage.isEmpty()) {
            //실제 구현 시 S3Uploader 등으로 업로드
            String dummyUrl = "https://example.com/dummy-profile.png";
            user.setProfileImage(dummyUrl);
        }

        userRepository.save(user); // 변경 감지 또는 save
    }
}
