package com.snapsplit.backend.feature.editTrip.dto;

import java.time.LocalDate;

public record UpdateScheduleRequest(
        LocalDate startDate,
        LocalDate endDate
) {}