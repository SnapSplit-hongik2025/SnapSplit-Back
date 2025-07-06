package com.snapsplit.backend.feature.upcomingTrips.service;

import com.snapsplit.backend.domain.country.entity.Country;
import com.snapsplit.backend.domain.country.repository.CountryRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.tripcountry.repository.TripCountryRepository;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.feature.upcomingTrips.dto.UpcomingTripResponse;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UpcomingTripsService {

    private final TripMemberRepository tripMemberRepository;
    private final TripCountryRepository tripCountryRepository;

    public List<UpcomingTripResponse> getUpcomingTrips(Long userId) {
        LocalDate today = LocalDate.now();
        List<Trip> trips = tripMemberRepository.findUpcomingTripsByUserId(userId, today);

        return trips.stream()
                .map(trip -> {
                    // Trip → TripCountry → Country 관계를 통해 국가 이름 추출
                    // 각 Trip의 국가 이름 목록은 TripCountry → Country → countryName으로 가져옴
                    List<String> countryNames = tripCountryRepository.findAllByTripId(trip.getId()).stream()
                            .map(tc -> tc.getCountry().getCountryName())
                            .collect(Collectors.toList());

                    return UpcomingTripResponse.from(trip, countryNames);
                })
                .collect(Collectors.toList());
    }
}
