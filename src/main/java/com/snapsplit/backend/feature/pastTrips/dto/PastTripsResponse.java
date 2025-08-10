package com.snapsplit.backend.feature.pastTrips.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PastTripsResponse {
    private List<PastTripResponse> trips;
    private Integer totalTrips;
    private Integer totalCountries;

    public static PastTripsResponse of(List<PastTripResponse> items, int totalTrips, int totalCountries) {
        return new PastTripsResponse(items, totalTrips, totalCountries);
    }
}
