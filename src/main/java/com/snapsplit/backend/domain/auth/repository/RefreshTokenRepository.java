package com.snapsplit.backend.domain.auth.repository;

import com.snapsplit.backend.domain.auth.token.RefreshToken;
import com.snapsplit.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user); // 해당 사용자의 refreshToken 삭제
}
