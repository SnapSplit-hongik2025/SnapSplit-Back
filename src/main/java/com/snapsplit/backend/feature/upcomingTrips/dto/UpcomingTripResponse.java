package com.snapsplit.backend.feature.upcomingTrips.dto;

import com.snapsplit.backend.domain.trip.entity.Trip;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class UpcomingTripResponse {

    private Long tripId;
    private String tripName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String tripImage;
    private BigDecimal tripTotalExpense;

    public static UpcomingTripResponse from(Trip trip) {
        return new UpcomingTripResponse(
                trip.getId(),
                trip.getTripName(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getTripImage(),
                trip.getTripTotalExpense()
        );
    }
}
