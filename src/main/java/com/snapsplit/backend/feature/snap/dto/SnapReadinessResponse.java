package com.snapsplit.backend.feature.snap.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SnapReadinessResponse {
    private boolean allMembersRegistered;
    private List<SnapMemberStatusDto> members;
}