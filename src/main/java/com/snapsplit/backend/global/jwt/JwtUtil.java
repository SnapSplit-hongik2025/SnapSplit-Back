package com.snapsplit.backend.global.jwt;

import com.snapsplit.backend.domain.user.entity.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
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

    // access token 생성
    public String generateAccessToken(User user) {
        return createToken(user.getKakaoId(), ACCESS_TOKEN_EXP, "access");
    }

    // refresh token 생성
    public String generateRefreshToken(User user) {
        return createToken(user.getKakaoId(), REFRESH_TOKEN_EXP, "refresh");
    }

    // jwt 생성
    private String createToken(String kakaoId, long expireTime, String tokenType) {
        return Jwts.builder()
                .setSubject(kakaoId)
                .claim("tokenType", tokenType)
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

    // jwt 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
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

    // token에서 토큰 타입(access / refresh) 추출 -> 이중 로그아웃 방지
    public String getTokenType(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("tokenType", String.class);
    }

}
