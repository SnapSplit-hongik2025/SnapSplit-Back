package com.snapsplit.backend.global.security;

import com.snapsplit.backend.global.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityUtil {

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("인증 정보 없음: SecurityContextHolder 비어있음");
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserPrincipal)) {
            log.error("예상하지 못한 인증 객체 타입: {}", principal.getClass());
            throw new UnauthorizedException("올바른 사용자 정보가 아닙니다.");
        }

        return ((CustomUserPrincipal) principal).getId();
    }
}
