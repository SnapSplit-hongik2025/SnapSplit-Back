package com.snapsplit.backend.feature.getTripCode.service;

import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTripCodeService {

    private final TripRepository tripRepository;

    @Transactional(readOnly = true)
    public String getTripCode(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("여행이 존재하지 않습니다."));
        return trip.getTripCode();
    }
}