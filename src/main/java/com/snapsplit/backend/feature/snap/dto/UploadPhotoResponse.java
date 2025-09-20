package com.snapsplit.backend.feature.snap.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class UploadPhotoResponse {

    private final Long photoId;
    private final String photoUrl;
    private final List<TaggedUser> taggedUsers;

    // 사진 메타데이터
    private final LocalDateTime takenAt;
    private final Double latitude;
    private final Double longitude;

    @Builder
    public UploadPhotoResponse(
            Long photoId,
            String photoUrl,
            List<TaggedUser> taggedUsers,
            LocalDateTime takenAt,
            Double latitude,
            Double longitude
    ) {
        this.photoId = photoId;
        this.photoUrl = photoUrl;
        this.taggedUsers = taggedUsers;
        this.takenAt = takenAt;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Getter
    public static class TaggedUser {
        private final Long userId;
        private final String userName;

        @Builder
        public TaggedUser(Long userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }
    }
}