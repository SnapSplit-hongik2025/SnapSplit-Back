package com.snapsplit.backend.feature.editTrip.dto;

import java.time.LocalDate;

public record ScheduleResponse(
        LocalDate startDate,
        LocalDate endDate
) {}
