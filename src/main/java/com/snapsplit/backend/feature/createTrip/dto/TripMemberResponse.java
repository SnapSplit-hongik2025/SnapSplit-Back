package com.snapsplit.backend.feature.createTrip.dto;

import com.snapsplit.backend.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TripMemberResponse {
    private Long id;
    private String name;
    private String profileImage;

    public static TripMemberResponse from(User user) {
        return new TripMemberResponse(
                user.getId(),
                user.getName(),
                user.getProfileImage()
        );
    }
}
