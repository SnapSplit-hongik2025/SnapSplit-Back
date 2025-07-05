package com.snapsplit.backend.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapsplit.backend.global.response.ApiResponse;
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

        if (isWhitelisted(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = jwtUtil.resolveToken(request);

        try {
            //블랙리스트 토큰인지 확인
            if (token != null && redisTemplate.hasKey(token)) {
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "로그아웃된 토큰입니다.");
                return;
            }

            if (token != null && jwtUtil.validateToken(token)) {
                String kakaoId = jwtUtil.getKakaoIdFromToken(token);

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(kakaoId, null, null);

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }


            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            ApiResponse<?> apiResponse = ApiResponse.fail(401, "토큰이 만료되었습니다.");
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));

        } catch (JwtException | IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            ApiResponse<?> apiResponse = ApiResponse.fail(401, "유효하지 않은 토큰입니다.");
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        }
    }

    private boolean isWhitelisted(String path) {
        return path.equals("/auth/kakao/login")
                || path.equals("/auth/token/refresh")
                || path.equals("/auth/kakao/logout");
    }


    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        ApiResponse<Object> errorResponse = ApiResponse.fail(status, message);
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}