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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 변환용

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        try {
            if (token != null && jwtUtil.validateToken(token)) {
                String kakaoId = jwtUtil.getKakaoIdFromToken(token);
                request.setAttribute("kakaoId", kakaoId);
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
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        ApiResponse<Object> errorResponse = ApiResponse.fail(status, message);
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}