package com.snapsplit.backend.domain.auth.service;

import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    // Spring Security가 JWT 검증 후
    // SecurityContext에 사용자를 넣을 때
    // UserDetailsService를 통해 사용자 정보 로딩

    private final UserRepository userRepository;

    // DB 안에서 사용자 정보 찾아서 UserDetails 객체로 변환
    @Override
    public UserDetails loadUserByUsername(String kakaoId) throws UsernameNotFoundException {
        User user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + kakaoId));

        return new org.springframework.security.core.userdetails.User(
                user.getKakaoId(),         // username
                "",                        // password (카카오 로그인이라 비워도 됨)
                List.of(new SimpleGrantedAuthority("ROLE_USER")) // 권한 부여
        );
    }
}
