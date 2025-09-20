package com.snapsplit.backend.feature.joinTrip.service;

import com.snapsplit.backend.feature.joinTrip.dto.JoinTripResponse;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.domain.tripmember.entity.MemberType;
import com.snapsplit.backend.domain.tripmember.entity.TripMember;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JoinTripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripMemberRepository tripMemberRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // 초대 코드로 여행 참여하기
    @Transactional
    public JoinTripResponse joinTrip(Long userId, String inviteCode) {
        Trip trip = tripRepository.findByTripCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("초대 코드에 해당하는 여행이 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        if (tripMemberRepository.existsByTripAndUser(trip, user)) {
            throw new IllegalArgumentException("이미 이 여행에 참여되어 있습니다.");
        }

        TripMember tripMember = TripMember.builder()
                .trip(trip)
                .user(user)
                .memberType(MemberType.USER)
                .build();
        tripMemberRepository.save(tripMember);

        // 여행 멤버가 바뀌었으므로 캐시 삭제
        String cacheKey = "trip::" + trip.getId() + "::snapReadiness";
        redisTemplate.delete(cacheKey);
        log.info("새 멤버 추가로 Snap 준비 상태 캐시 삭제 완료: {}", cacheKey);

        return JoinTripResponse.builder()
                .tripId(trip.getId())
                .build();
    }
}
