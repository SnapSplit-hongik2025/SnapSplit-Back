package com.snapsplit.backend.feature.joinTrip.service;

import com.snapsplit.backend.feature.joinTrip.dto.JoinTripResult;
import org.springframework.transaction.annotation.Transactional;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.domain.tripmember.entity.MemberType;
import com.snapsplit.backend.domain.tripmember.entity.TripMember;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import com.snapsplit.backend.feature.joinTrip.dto.JoinTripResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class JoinTripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripMemberRepository tripMemberRepository;

    // 초대 코드로 여행 참여하기
    @Transactional
    public JoinTripResult joinTrip(Long userId, String inviteCode) {
        Trip trip = tripRepository.findByTripCode(inviteCode).orElse(null);

        // 초대 코드에 해당하는 여행이 없을 시
        if (trip == null) {
            return JoinTripResult.builder()
                    .success(false)
                    .message("초대 코드에 해당하는 여행이 없습니다.")
                    .build();
        }

        // 사용자 정보를 찾을 수 없을 시
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return JoinTripResult.builder()
                    .success(false)
                    .message("사용자 정보를 찾을 수 없습니다.")
                    .build();
        }

        // 이미 여행에 참여된 사용자가 또 초대 코드로 입력했을 시
        boolean alreadyJoined = tripMemberRepository.existsByTripAndUser(trip, user);
        if (alreadyJoined) {
            return JoinTripResult.builder()
                    .success(false)
                    .message("이미 이 여행에 참여되어 있습니다.")
                    .build();
        }

        // 새 TripMember 생성
        TripMember tripMember = TripMember.builder()
                .trip(trip)
                .user(user)
                .memberType(MemberType.USER)
                .build();
        tripMemberRepository.save(tripMember);

        JoinTripResponse response = JoinTripResponse.builder()
                .tripId(trip.getId())
                .build();

        return JoinTripResult.builder()
                .success(true)
                .message("여행에 성공적으로 참여가 완료되었습니다.")
                .data(response)
                .build();
    }

}
