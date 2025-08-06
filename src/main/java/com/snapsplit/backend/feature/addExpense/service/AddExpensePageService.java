package com.snapsplit.backend.feature.addExpense.service;

import com.snapsplit.backend.domain.totalshared.entity.TotalShared;
import com.snapsplit.backend.domain.totalshared.repository.TotalSharedRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.domain.tripcountry.repository.TripCountryRepository;
import com.snapsplit.backend.domain.tripmember.entity.TripMember;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.feature.addExpense.dto.AddExpensePageResponse;
import com.snapsplit.backend.feature.getExchangeRate.dto.ExchangeRateResponse;
import com.snapsplit.backend.feature.getExchangeRate.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddExpensePageService {

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripCountryRepository tripCountryRepository;
    private final ExchangeRateService exchangeRateService;

    public AddExpensePageResponse getAddExpensePageData(Long tripId, LocalDate date) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

        // 1. 대표 통화
        String defaultCurrency = trip.getDefaultCurrency();

        // 2. 사용 가능한 통화 (여행 국가의 통화 + 무조건 KRW 포함)
        Set<String> currencySet = tripCountryRepository.findAllByTripId(trip.getId()).stream()
                .map(tc -> tc.getCountry().getCurrency())
                .collect(Collectors.toSet());
        currencySet.add("KRW"); // 항상 포함
        List<String> availCurrencies = new ArrayList<>(currencySet);

        // 3. 환율 정보 조회 (모든 통화에 대해)
        Map<String, BigDecimal> exchangeRates = new HashMap<>();
        ExchangeRateResponse rateResponse = exchangeRateService.fetchExchangeRate(availCurrencies);
        for (ExchangeRateResponse.ExchangeRateItem item : rateResponse.getRates()) {
            exchangeRates.put(item.getCode(), item.getRateToBase());
        }


        // 4. 여행 멤버 목록
        List<AddExpensePageResponse.MemberDto> members = tripMemberRepository.findAllByTripId(trip.getId()).stream()
                .map(tm -> AddExpensePageResponse.MemberDto.builder()
                        .memberId(tm.getId())
                        .name(tm.getUser() != null ? tm.getUser().getName() : "공동경비")
                        .memberType(tm.getUser() == null ? "SHARED_FUND" : "USER")
                        .build())
                .toList();

        // 5. 최종 응답 구성
        return AddExpensePageResponse.builder()
                .defaultCurrency(defaultCurrency)
                .availCurrencies(availCurrencies)
                .exchangeRates(exchangeRates)
                .members(members)
                .defaultDate(date.toString())
                .build();
    }
}
