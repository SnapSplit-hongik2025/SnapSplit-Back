package com.snapsplit.backend.feature.pastTrips.service;

import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.tripcountry.repository.TripCountryRepository;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.feature.pastTrips.dto.PastTripResponse;
import com.snapsplit.backend.feature.pastTrips.dto.PastTripsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PastTripsService {

    private final TripMemberRepository tripMemberRepository;
    private final TripCountryRepository tripCountryRepository;

    // 기본 홈 지난 여행 미리보기 - limit 적용
    public List<PastTripResponse> getPastTripsPreview(Long userId, Integer limit) {
        LocalDate today = LocalDate.now();
        var pageable = PageRequest.of(0, limit);

        // TripMember를 통해 userId가 참여한 Trip 중, 지난 여행(endDate < today)만 조회함
        List<Trip> trips = tripMemberRepository.findPastTripsByUserId(userId, today, pageable);

        return trips.stream()
                .map(trip -> {
                    // Trip → TripCountry → Country 연관관계를 따라 국가 이름 목록 추출
                    List<String> countryNames = tripCountryRepository.findAllByTripId(trip.getId()).stream()
                            .map(tc -> tc.getCountry().getCountryName())
                            .toList();
                    return PastTripResponse.from(trip, countryNames);
                })
                .toList();
    }

    // 지난 여행 전체보기 - 전체 여행 수, 여행 국가 수 포함 envelope 반환
    public PastTripsResponse getPastTripsWithStats(Long userId) {
        LocalDate today = LocalDate.now();
        // 전체 조회
        List<Trip> trips = tripMemberRepository.findPastTripsByUserId(userId, today, Pageable.unpaged());

        List<PastTripResponse> items = trips.stream()
                .map(trip -> {
                    List<String> countryNames = tripCountryRepository.findAllByTripId(trip.getId()).stream()
                            .map(tc -> tc.getCountry().getCountryName())
                            .toList();
                    return PastTripResponse.from(trip, countryNames);
                })
                .toList();

        int totalTrips = items.size();
        int totalCountries = items.stream()
                .flatMap(t -> t.getCountryNames().stream())
                .collect(java.util.stream.Collectors.toSet())
                .size();

        return PastTripsResponse.of(items, totalTrips, totalCountries);
    }
}
