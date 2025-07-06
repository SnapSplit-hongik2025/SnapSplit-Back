package com.snapsplit.backend.feature.upcomingTrips.service;

import com.snapsplit.backend.domain.trip.entity.Trip;
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

    private final TripRepository tripRepository;

    public List<UpcomingTripResponse> getUpcomingTrips(Long userId) {
        LocalDate today = LocalDate.now();
        List<Trip> trips = tripRepository.findUpcomingTripsByUserId(userId, today);
        return trips.stream()
                .map(UpcomingTripResponse::from)
                .collect(Collectors.toList());
    }
}
