package com.snapsplit.backend.feature.createTrip.service;

import com.snapsplit.backend.domain.user.entity.User;
import com.snapsplit.backend.domain.user.repository.UserRepository;
import com.snapsplit.backend.feature.createTrip.dto.TripMemberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TripMemberService {

    private final UserRepository userRepository;

    // 유저 코드로 유저 검색
    public TripMemberResponse findByUserCode(String userCode, Long currentUserId) {
        Optional<User> targetUserOptional = userRepository.findByUserCode(userCode);

        // 유저 코드로 검색한 결과가 없거나 검색 결과가 사용자 본인이면 null 처리
        if (targetUserOptional.isEmpty() || targetUserOptional.get().getId().equals(currentUserId)) {
            return null;
        }

        User targetUser = targetUserOptional.get();
        return TripMemberResponse.from(targetUser);
    }
}
