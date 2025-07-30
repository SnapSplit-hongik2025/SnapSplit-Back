package com.snapsplit.backend.global.aop;

import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.global.exception.ForbiddenException;
import com.snapsplit.backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TripAuthorizationAspect {

    private final TripMemberRepository tripMemberRepository;
    private final SecurityUtil securityUtil;

    @Pointcut("@annotation(com.snapsplit.backend.global.aop.CheckTripMember)")
    public void checkTripMemberPointcut() {}

    @Around("checkTripMemberPointcut()")
    public Object validateTripMember(ProceedingJoinPoint joinPoint) throws Throwable {
        // 파라미터 이름, 값 모두 가져오기
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();

        Long tripId = null;

        for (int i = 0; i < paramNames.length; i++) {
            if ("tripId".equals(paramNames[i]) && args[i] instanceof Long) {
                tripId = (Long) args[i];
                break;
            }
        }

        if (tripId == null) {
            throw new IllegalArgumentException("tripId 파라미터를 찾을 수 없습니다.");
        }

        Long userId = securityUtil.getCurrentUserId();

        Trip trip = Trip.builder().id(tripId).build();
        User user = User.builder().id(userId).build();

        boolean isMember = tripMemberRepository.existsByTripAndUser(trip, user);

        if (!isMember) {
            throw new ForbiddenException("해당 여행에 접근할 권한이 없습니다.");
        }

        return joinPoint.proceed();
    }
}
