package com.snapsplit.backend.feature.getExchangeRate.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HolidayService {
    private Set<LocalDate> holidays = new HashSet<>();

    @PostConstruct
    public void loadHolidays() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = getClass().getResourceAsStream("/holidays-2025.json");
        List<String> dates = mapper.readValue(is, new TypeReference<>() {});
        holidays = dates.stream().map(LocalDate::parse).collect(Collectors.toSet());
    }

    public boolean isHoliday(LocalDate date) {
        return holidays.contains(date);
    }
}
