package com.snapsplit.backend.config;

import com.snapsplit.backend.domain.auth.handler.OAuth2LoginSuccessHandler;
import com.snapsplit.backend.domain.auth.service.CustomOAuth2UserService;
import com.snapsplit.backend.domain.auth.service.CustomUserDetailsService;
import com.snapsplit.backend.domain.auth.service.TokenBlacklistService;
import com.snapsplit.backend.global.jwt.JwtAuthenticationFilter;
import com.snapsplit.backend.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 카카오 로그인 시 사용자 정보 받아오는 서비스
    private final CustomOAuth2UserService customOAuth2UserService;

    // 로그인 성공 후 JWT 생성해서 리다이렉트 처리
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    // JWT 생성, 검증 도구
    private final JwtUtil jwtUtil;

    // JWT에서 사용자 로드할 때 사용하는 서비스
    private final CustomUserDetailsService userDetailsService;

    private final TokenBlacklistService tokenBlacklistService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // REST API 기반이라 CSRF 보호 꺼도 됨
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT라서 세션 사용 안 함
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/kakao/**").permitAll() // /auth/kakao 는 인증 없이 접근 가능
                        .anyRequest().authenticated() // 나머지 경로는 인증 필요
                )
                // Spring Security의 소셜 로그인 처리 영역
                .oauth2Login(oauth -> oauth
                        .successHandler(oAuth2LoginSuccessHandler) // 로그인 성공 시 실행되는 로직 지정
                        .userInfoEndpoint(user -> user.userService(customOAuth2UserService)) // 카카오에서 받아온 사용자 정보 가공
                )
                // JWT로 인증되면, 그 이후 기본 로그인 필터는 굳이 건들 필요 없다는 의미..
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService, tokenBlacklistService), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
