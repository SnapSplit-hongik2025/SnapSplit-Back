package com.snapsplit.backend.feature.home.service;

import com.snapsplit.backend.feature.home.dto.HomeResponse;
import com.snapsplit.backend.feature.pastTrips.service.PastTripsService;
import com.snapsplit.backend.feature.upcomingTrips.service.UpcomingTripsService;
import com.snapsplit.backend.feature.pastTrips.dto.PastTripResponse;
import com.snapsplit.backend.feature.upcomingTrips.dto.UpcomingTripResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final PastTripsService pastTripsService;
    private final UpcomingTripsService upcomingTripsService;

    public HomeResponse getHome(Long userId) {
        // 지난 여행: limit 5
        List<PastTripResponse> pastTrips = pastTripsService.getPastTrips(userId, 5);
        List<UpcomingTripResponse> upcomingTrips = upcomingTripsService.getUpcomingTrips(userId);

        return HomeResponse.builder()
                .upcomingTrips(upcomingTrips)
                .pastTrips(pastTrips)
                .build();
    }
}
