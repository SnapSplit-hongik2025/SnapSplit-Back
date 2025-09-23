package com.snapsplit.backend.feature.snap.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.snapsplit.backend.domain.photo.entity.Photo;
import com.snapsplit.backend.domain.phototag.entity.PhotoTag;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
public class PhotoPageResponse {
    // 사진 정보 리스트
    private List<PhotoDetailDto> photos;
    // 페이징 정보
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private boolean isLast;

    @Getter
    @Builder
    public static class PhotoDetailDto {
        private Long photoId;
        private String photoUrl;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate photoDate;
        private List<TaggedUserDto> taggedUsers;
    }

    @Getter
    @Builder
    public static class TaggedUserDto {
        private Long userId;
        private String name;
    }
}
