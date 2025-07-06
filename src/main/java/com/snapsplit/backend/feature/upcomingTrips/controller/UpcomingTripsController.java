package com.snapsplit.backend.feature.upcomingTrips.controller;

import com.snapsplit.backend.feature.upcomingTrips.dto.UpcomingTripResponse;
import com.snapsplit.backend.feature.upcomingTrips.service.UpcomingTripsService;
import com.snapsplit.backend.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trips")
public class UpcomingTripsController {

    private final UpcomingTripsService upcomingTripsService;

    @GetMapping("/upcoming")
    public ResponseEntity<List<UpcomingTripResponse>> getUpcomingTrips(
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {
        List<UpcomingTripResponse> trips = upcomingTripsService.getUpcomingTrips(user.getId());
        return ResponseEntity.ok(trips);
    }
}
