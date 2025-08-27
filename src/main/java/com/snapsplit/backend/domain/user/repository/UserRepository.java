package com.snapsplit.backend.domain.user.repository;

import com.snapsplit.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 카카오 아이디로 사용자 찾기
    Optional<User> findByKakaoId(String kakaoId);

    // 유저 코드로 사용자 찾기
    Optional<User> findByUserCode(String userCode);

    // AWS Rekognition FaceId로 사용자를 찾는 메소드
    Optional<User> findByAwsFaceId(String awsFaceId);
}
