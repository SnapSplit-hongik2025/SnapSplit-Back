package com.snapsplit.backend.feature.settlement.service;

import com.snapsplit.backend.domain.settlement.repository.SettlementRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.feature.settlement.dto.SettlementPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementPageService {

    private final TripRepository tripRepository;
    private final SettlementRepository settlementRepository;

    public SettlementPageResponse getSettlementPage(Long tripId) {

        // 여행 찾기
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("여행이 존재하지 않습니다."));

        // 정산 내역 정보
        List<SettlementPageResponse.SettlementSummary> completeSettlement = settlementRepository.findAllByTripId(tripId).stream()
                .map(settlement -> SettlementPageResponse.SettlementSummary.builder()
                        .id(settlement.getId())
                        .startDate(settlement.getStartDate())
                        .endDate(settlement.getEndDate())
                        .build())
                .toList();

        // 여행 시작일 + 정산 내역 정보 return
        return SettlementPageResponse.builder()
                .trip(SettlementPageResponse.TripInfo.builder()
                        .startDate(trip.getStartDate())
                        .endDate(trip.getEndDate())
                        .build())
                .completeSettlement(completeSettlement)
                .build();
    }
}
