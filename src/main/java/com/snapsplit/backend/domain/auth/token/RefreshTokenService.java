package com.snapsplit.backend.domain.auth.token;

import com.snapsplit.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void save(User user, String token, LocalDateTime expiresAt) {
        // 기존 토큰이 있는지 조회
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);

        if (existingToken.isPresent()) {
            // 갱신 (update)
            RefreshToken refreshToken = existingToken.get();
            refreshToken.setToken(token);
            refreshToken.setExpiresAt(expiresAt);
            // 엔티티는 변경 감지(dirty checking)로 자동 저장됨
        } else {
            // 새로 저장 (insert)
            RefreshToken newToken = RefreshToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(expiresAt)
                    .build();
            refreshTokenRepository.save(newToken);
        }
    }

    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteAllByUser(user);
    }

    @Transactional
    public boolean deleteByToken(String token) {

        Optional<RefreshToken> tokenEntity = refreshTokenRepository.findByToken(token);
        if (tokenEntity.isPresent()) {
            refreshTokenRepository.deleteByToken(token);
            return true;
        } else {
            return false;
        }

    }

    @Transactional(readOnly = true)
    public boolean isValid(String token) {
        return refreshTokenRepository.findByToken(token)
                .map(rt -> !rt.isExpired())
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

}
