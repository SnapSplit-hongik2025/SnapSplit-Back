package com.snapsplit.backend.feature.myPage.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyFaceResponse {
    private final boolean registered;
    private final String faceImageUrl;
}
