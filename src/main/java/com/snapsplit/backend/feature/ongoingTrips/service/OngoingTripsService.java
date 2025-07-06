package com.snapsplit.backend.feature.ongoingTrips.service;

import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.tripcountry.repository.TripCountryRepository;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.feature.ongoingTrips.dto.OngoingTripResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OngoingTripsService {

    private final TripMemberRepository tripMemberRepository;
    private final TripCountryRepository tripCountryRepository;

    public List<OngoingTripResponse> getOngoingTrips(Long userId) {
        LocalDate today = LocalDate.now();

        // 진행 중인 여행 조회
        List<Trip> trips = tripMemberRepository.findOngoingTripsByUserId(userId, today);

        // Trip → OngoingTripResponse 변환
        return trips.stream()
                .map(trip -> {
                    List<String> countryNames = tripCountryRepository.findAllByTripId(trip.getId()).stream()
                            .map(tc -> tc.getCountry().getCountryName())
                            .collect(Collectors.toList());

                    return OngoingTripResponse.from(trip, countryNames);
                })
                .collect(Collectors.toList());
    }
}
