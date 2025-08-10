package com.snapsplit.backend.feature.addExpense.service;

import com.snapsplit.backend.domain.settlement.entity.Settlement;
import com.snapsplit.backend.domain.settlement.repository.SettlementRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.domain.tripcountry.repository.TripCountryRepository;
import com.snapsplit.backend.domain.tripmember.repository.TripMemberRepository;
import com.snapsplit.backend.feature.addExpense.dto.AddExpensePageResponse;
import com.snapsplit.backend.feature.getExchangeRate.dto.ExchangeRateResponse;
import com.snapsplit.backend.feature.getExchangeRate.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddExpensePageService {

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripCountryRepository tripCountryRepository;
    private final ExchangeRateService exchangeRateService;
    private final SettlementRepository settlementRepository;

    public AddExpensePageResponse getAddExpensePageData(Long tripId, LocalDate date) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

        // 여행 기간 밖 접근 차단
        LocalDate tripStart = trip.getStartDate();
        LocalDate tripEnd   = trip.getEndDate();
        LocalDate preTrip   = tripStart.minusDays(1);

        // 여행 시작 전 날짜로 접근시 모두 preTrip로 고정
        LocalDate effectiveDate = date.isBefore(tripStart) ? preTrip : date;
        // 여행 종료일 이후면 400에러
        if (effectiveDate.isAfter(tripEnd)) {
            throw new IllegalArgumentException(
                    "여행 기간 밖의 날짜에는 지출을 추가할 수 없습니다.");
        }
        // 정규화된 날짜가 정산 구간에 포함되면 400
        boolean isSettled = settlementRepository
                .existsByTrip_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(tripId, effectiveDate, effectiveDate);
        if (isSettled) {
            throw new IllegalArgumentException("이미 정산된 날짜에는 지출을 추가할 수 없습니다.");
        }

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

        // 5. 정산 완료 날짜 오름차순 생성
        List<Settlement> settlements = settlementRepository.findAllByTripId(tripId);SortedSet<LocalDate> settledSet = new TreeSet<>();
        for (Settlement s : settlements) {
            settledSet.addAll(datesBetweenInclusive(s.getStartDate(), s.getEndDate()));
        }

        List<String> settledDates = settledSet.stream()
                .map(LocalDate::toString)
                .toList();

        // 6. 최종 응답 구성
        return AddExpensePageResponse.builder()
                .defaultCurrency(defaultCurrency)
                .availCurrencies(availCurrencies)
                .exchangeRates(exchangeRates)
                .members(members)
                .defaultDate(effectiveDate.toString())
                .settledDates(settledDates)
                .build();
    }

    // [start, end] 포함 범위 날짜 리스트
    private List<LocalDate> datesBetweenInclusive(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) return List.of();
        long days = ChronoUnit.DAYS.between(start, end);
        List<LocalDate> out = new ArrayList<>((int) days + 1);
        for (long i = 0; i <= days; i++) out.add(start.plusDays(i));
        return out;
    }
}
