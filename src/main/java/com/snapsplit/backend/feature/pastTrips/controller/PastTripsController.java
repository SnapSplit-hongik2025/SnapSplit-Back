package com.snapsplit.backend.feature.pastTrips.controller;

import com.snapsplit.backend.feature.pastTrips.dto.PastTripResponse;
import com.snapsplit.backend.feature.pastTrips.service.PastTripsService;
import com.snapsplit.backend.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trips")
public class PastTripsController {

    private final PastTripsService pastTripsService;

    @GetMapping("/past")
    public ResponseEntity<List<PastTripResponse>> getPastTrips(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @RequestParam(required = false) Integer limit
    ) {
        List<PastTripResponse> trips = pastTripsService.getPastTrips(user.getId(), limit);
        return ResponseEntity.ok(trips);
    }
}
