package com.snapsplit.backend.feature.snap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class UpdatePhotoTagRequest {
    private List<Long> memberIds;
}