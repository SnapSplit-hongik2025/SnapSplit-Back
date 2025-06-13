package com.snapsplit.backend.domain.auth.token;

import com.snapsplit.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 여러 개의 refresh token이 하나의 사용자에 연결 가능
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // jwt 문자열
    @Column(nullable = false, unique = true, length = 500)
    private String token;

    // refresh token의 만료 시간
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
