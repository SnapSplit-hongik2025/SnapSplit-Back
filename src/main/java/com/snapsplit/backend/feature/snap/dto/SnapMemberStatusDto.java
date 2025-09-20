package com.snapsplit.backend.feature.snap.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonPropertyOrder({"userId", "name", "profileImageUrl", "hasFaceData", "isCurrentUser"})
public class SnapMemberStatusDto {
    private Long userId;
    private String name;
    private String profileImageUrl;
    private boolean hasFaceData;
    private boolean isCurrentUser;
}