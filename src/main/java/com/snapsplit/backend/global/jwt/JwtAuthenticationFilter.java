package com.snapsplit.backend.global.jwt;

import com.snapsplit.backend.domain.auth.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 모든 요청에 대해 JWT 토큰을 검사하고 인증 객체를 설정하는 필터

    private final JwtUtil jwtUtil; // jwt 검증 및 파싱
    private final UserDetailsService userDetailsService; // jwt에서 꺼낸 카카오 id 이용해서 사용자 정보 조회
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // http 요청에서 authorization : Bearer <token> 헤더 꺼냄
        // resolveToken() 으로 순수 JWT만 반환
        String token = resolveToken(request);

        // 블랙리스트에 있는 토큰인지 먼저 확인
        if (token != null && tokenBlacklistService.isTokenBlacklisted(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\": \"블랙리스트 처리된 토큰입니다.\"}");
            return;
        }

        // 토큰이 존재하고, 유효할 경우
        if (token != null && jwtUtil.validateToken(token)) {

            // jwt에서 사용자 id 추출
            String kakaoId = jwtUtil.getKakaoIdFromToken(token);

            // userdetail 객체로 사용자 정보 변환해서 받기
            UserDetails userDetails = userDetailsService.loadUserByUsername(kakaoId);

            // 인증 정보 등록
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    // 순수 jwt 파싱하는 함수
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
