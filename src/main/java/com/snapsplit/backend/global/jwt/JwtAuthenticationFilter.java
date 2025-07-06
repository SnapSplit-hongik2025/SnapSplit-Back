package com.snapsplit.backend.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapsplit.backend.global.response.ApiResponse;
import com.snapsplit.backend.global.security.CustomUserPrincipal;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 변환용

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // 화이트리스트 경로는 필터 건너뛰기
        if (isWhitelisted(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = jwtUtil.resolveToken(request);

        try {
            if (token == null) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Access Token이 필요합니다.");
                return;
            }

            // 로그아웃된 토큰 여부 확인 (Redis에 저장되어 있음)
            if (redisTemplate.hasKey(token)) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "로그아웃된 토큰입니다.");
                return;
            }

            // 유효성 검사: 만료, 불일치 등은 내부에서 예외 발생
            jwtUtil.validateToken(token);

            // 토큰에서 사용자 정보 추출
            Long userId = jwtUtil.getUserIdFromToken(token);
            String kakaoId = jwtUtil.getKakaoIdFromToken(token);
            String nickname = jwtUtil.getNicknameFromToken(token);

            // 인증 객체 생성 및 SecurityContext에 등록
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, kakaoId, nickname);
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            // 다음 필터로 진행
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            //토큰 만료 -> 401
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "토큰이 만료되었습니다.");
        } catch (JwtException | IllegalArgumentException e) {
            //잘못된 토큰 -> 401
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }
    }

    //토큰 인증 없이 접근 가능한 경로는 로그인과 토큰재발급뿐
    private boolean isWhitelisted(String path) {
        return path.equals("/auth/kakao/login")
                || path.equals("/auth/token/refresh");
    }

    //에러 응답 공통 처리 함수
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        ApiResponse<Object> errorResponse = ApiResponse.fail(status, message);
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
