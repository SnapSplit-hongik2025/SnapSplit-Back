package com.snapsplit.backend.domain.auth.token;

import com.snapsplit.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void save(User user, String token, LocalDateTime expiresAt) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public boolean isValid(String token) {
        return refreshTokenRepository.findByToken(token)
                .map(rt -> !rt.isExpired())  // 엔티티에 isExpired() 메서드 있다고 가정
                .orElse(false);
    }

    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteAllByUser(user);
    }
}
