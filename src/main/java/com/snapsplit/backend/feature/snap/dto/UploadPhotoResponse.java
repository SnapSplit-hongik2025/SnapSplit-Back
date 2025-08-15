package com.snapsplit.backend.feature.snap.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
public class UploadPhotoResponse {

    private final Long photoId;
    private final String photoUrl;
    private final List<TaggedUser> taggedUsers;

    @Builder
    public UploadPhotoResponse(Long photoId, String photoUrl, List<TaggedUser> taggedUsers) {
        this.photoId = photoId;
        this.photoUrl = photoUrl;
        this.taggedUsers = taggedUsers;
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