package com.snapsplit.backend.feature.pastTrips.service;

import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.tripcountry.repository.TripCountryRepository;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.feature.pastTrips.dto.PastTripResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PastTripsService {

    private final TripMemberRepository tripMemberRepository;
    private final TripCountryRepository tripCountryRepository;

    public List<PastTripResponse> getPastTrips(Long userId, Integer limit) {
        LocalDate today = LocalDate.now();

        // limit이 지정되면 앞에서부터 limit개만 조회, 아니면 전체 조회
        Pageable pageable = (limit != null) ? PageRequest.of(0, limit) : Pageable.unpaged();

        // TripMember를 통해 userId가 참여한 Trip 중, 지난 여행(endDate < today)만 조회함
        List<Trip> trips = tripMemberRepository.findPastTripsByUserId(userId, today, pageable);

        return trips.stream()
                .map(trip -> {
                    // Trip → TripCountry → Country 연관관계를 따라 국가 이름 목록 추출
                    List<String> countryNames = tripCountryRepository.findAllByTripId(trip.getId()).stream()
                            .map(tc -> tc.getCountry().getCountryName())
                            .collect(Collectors.toList());

                    return PastTripResponse.from(trip, countryNames);
                })
                .collect(Collectors.toList());
    }
}
