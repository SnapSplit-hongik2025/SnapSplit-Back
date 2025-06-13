package com.snapsplit.backend.domain.user.repository;

import com.snapsplit.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKakaoId(String kakaoId); // ← 이렇게 메서드만 써주면 끝!
}
