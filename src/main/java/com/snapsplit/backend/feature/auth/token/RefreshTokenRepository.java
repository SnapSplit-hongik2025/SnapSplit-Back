package com.snapsplit.backend.feature.auth.token;

import com.snapsplit.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUser(User user);


    void deleteAllByUser(User user);
    void deleteByToken(String token);
}
