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

    @Around("checkTripMemberPointcut() && args(tripId,..)")
    public Object validateTripMember(ProceedingJoinPoint joinPoint, Long tripId) throws Throwable {
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
