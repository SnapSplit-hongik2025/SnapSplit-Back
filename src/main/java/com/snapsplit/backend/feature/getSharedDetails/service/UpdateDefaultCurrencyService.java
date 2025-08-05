package com.snapsplit.backend.feature.getSharedDetails.service;

import com.snapsplit.backend.domain.totalshared.entity.TotalShared;
import com.snapsplit.backend.domain.totalshared.repository.TotalSharedRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.feature.getSharedDetails.dto.UpdateDefaultCurrencyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UpdateDefaultCurrencyService {

    private final TripRepository tripRepository;
    private final TotalSharedRepository totalSharedRepository;
    @Transactional
    public UpdateDefaultCurrencyResponse updateDefaultCurrency(Long tripId, String newCurrency) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

        String before = trip.getDefaultCurrency(); // 기존 대표 통화값
        tripRepository.updateDefaultCurrencyById(tripId, newCurrency); // 대표 통화 변경

        // 이용 가능한 통화 목록
        List<String> availCurrencies = totalSharedRepository.findByTrip(trip).stream()
                .map(TotalShared::getTotalSharedCurrency)
                .distinct()
                .toList();

        // 이용 가능한 통화 목록에 포함되어 있지 않다면
        if (!availCurrencies.contains(newCurrency)) {
            throw new IllegalArgumentException("선택한 통화는 현재 공동경비에 사용되지 않았습니다.");
        }
        
        return UpdateDefaultCurrencyResponse.builder()
                .before(before)
                .after(newCurrency)
                .build();
    }

}
