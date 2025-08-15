package com.snapsplit.backend.feature.snap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class DeletePhotoRequest { // 클래스 이름 변경
    private List<Long> photoIds;
}