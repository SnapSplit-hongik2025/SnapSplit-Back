package com.snapsplit.backend.global.jwt;

import com.snapsplit.backend.domain.user.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtUtil {

    // jwt 생성, 검증, 파싱 담당

    private final Key key;
    private final long ACCESS_TOKEN_EXP;
    private final long REFRESH_TOKEN_EXP;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-exp}") long accessExp,
            @Value("${jwt.refresh-exp}") long refreshExp
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.ACCESS_TOKEN_EXP = accessExp;
        this.REFRESH_TOKEN_EXP = refreshExp;
    }

    public String generateAccessToken(User user) {
        return createToken(user.getKakaoId(), ACCESS_TOKEN_EXP, "access", user.getId(), user.getName());
    }

    public String generateRefreshToken(User user) {
        return createToken(user.getKakaoId(), REFRESH_TOKEN_EXP, "refresh", user.getId(), user.getName());
    }


    // jwt 생성
    private String createToken(String kakaoId, long expireTime, String tokenType, Long userId, String nickname) {
        return Jwts.builder()
                .setSubject(kakaoId)
                .claim("id", userId) // 사용자 아이디
                .claim("nickname", nickname) // 사용자 닉네임
                .claim("tokenType", tokenType) // 토큰 타입
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    // token에서 kakaoId 추출
    public String getKakaoIdFromToken(String token) {
        //System.out.println("TOKEN = [" + token + "]");
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // token에서 userId 추출
    public Long getUserIdFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("id", Long.class);
    }

    // token에서 nickname 추출
    public String getNicknameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("nickname", String.class);
    }


    // jwt 유효성 검사 boolean -> void로 바꿔서 에러정보 상세화
    public void validateToken(String token) {
        Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    // 리프레시 토큰 만료 시간
    public LocalDateTime getRefreshTokenExpiry() {
        return Instant.now()
                .plusMillis(REFRESH_TOKEN_EXP)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    //jwt 만료 시간 추출 유틸 함수
    public Date getExpiration(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }


    public String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    // token에서 토큰 타입(access / refresh) 추출 -> 이중 로그아웃 방지
    public String getTokenType(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("tokenType", String.class);
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이라도 내부 claims에는 접근 가능
            return e.getClaims().get("tokenType", String.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 토큰 형식입니다.");
        }
    }

}