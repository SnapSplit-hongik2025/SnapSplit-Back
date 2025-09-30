package com.snapsplit.backend.feature.getSharedDetails.service;

import com.snapsplit.backend.domain.expense.entity.Expense;
import com.snapsplit.backend.domain.expense.repository.ExpenseRepository;
import com.snapsplit.backend.domain.shared.entity.Shared;
import com.snapsplit.backend.domain.shared.entity.SharedType;
import com.snapsplit.backend.domain.shared.repository.SharedRepository;
import com.snapsplit.backend.domain.totalshared.entity.TotalShared;
import com.snapsplit.backend.domain.totalshared.repository.TotalSharedRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.feature.getSharedDetails.dto.SharedDetailsResponse;
import com.snapsplit.backend.feature.getSharedDetails.dto.SharedDetailsResponse.SharedDayGroup;
import com.snapsplit.backend.feature.getSharedDetails.dto.SharedDetailsResponse.SharedItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetSharedDetailsService {

    private final TripRepository tripRepository;
    private final SharedRepository sharedRepository;
    private final ExpenseRepository expenseRepository;
    private final TotalSharedRepository totalSharedRepository;

    @Transactional(readOnly = true)
    public SharedDetailsResponse getSharedDetails(Long tripId) {

        // 여행 검색
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

        // 대표통화 값 받아오기
        String tripStartDate = trip.getStartDate().toString();
        String defaultCurrency = trip.getDefaultCurrency();

        // 여행과 통화가 일치하는 공동경비 세부내역 검색
        List<Shared> sharedList = sharedRepository.findByTripAndCurrency(trip, defaultCurrency);
        Map<String, List<SharedItem>> sharedItemMap = sharedList.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getCreatedAt().toString(), // 여기 s 선언됨
                        LinkedHashMap::new,
                        Collectors.mapping(s -> {
                            String type = s.getSharedType().name();
                            String title = null;
                            String memo = null;
                            BigDecimal amount = s.getAmount();
                            BigDecimal amountKRW = s.getAmountKRW();

                            // EXPENSE인 경우
                            if (s.getExpenseId() != null) {
                                Expense e = expenseRepository.findById(s.getExpenseId())
                                        .orElse(null);
                                if (e != null) {
                                    type = SharedType.EXPENSE.name();
                                    title = e.getExpenseName();
                                    memo = e.getExpenseMemo();
                                } else {
                                    // expense를 찾을 수 없는 경우
                                    type = SharedType.EXPENSE.name();
                                    title = "알 수 없는 지출";
                                    memo = "";
                                }
                            } else {
                                // DEPOSIT인 경우
                                if(s.getSharedType() == SharedType.DEPOSIT) {
                                    type = SharedType.DEPOSIT.name();
                                    title = "공동경비 입금";
                                    memo = "";
                                }
                                // WITHDRAW인 경우
                                else if(s.getSharedType() == SharedType.WITHDRAW) {
                                    type = SharedType.WITHDRAW.name();
                                    title = "공동경비 출금";
                                    memo = "";
                                }
                            }

                            return SharedItem.builder()
                                    .type(type)
                                    .title(title)
                                    .memo(memo)
                                    .amount(amount)
                                    .amountKRW(amountKRW)
                                    .build();
                        }, Collectors.toList())
                ));


        // 날짜 기준 병합
        Set<String> allDates = new TreeSet<>();
        allDates.addAll(sharedItemMap.keySet());

        List<SharedDayGroup> dayGroups = allDates.stream()
                .map(date -> {
                    LocalDate localDate = LocalDate.parse(date);
                    List<SharedItem> combined = sharedItemMap.getOrDefault(date, new ArrayList<>());

                    return SharedDayGroup.builder()
                            .date(date)
                            .items(combined)
                            .build();
                })
                .toList();


        // 공동경비 통화별 잔액 리스트 가져오기
        List<TotalShared> totalSharedList = totalSharedRepository.findByTrip(trip);

        List<SharedDetailsResponse.CurrencyAmount> totalSharedBudget = totalSharedList.stream()
                .map(t -> SharedDetailsResponse.CurrencyAmount.builder()
                        .currency(t.getTotalSharedCurrency())
                        .amount(t.getTotalSharedAmount())
                        .build())
                .toList();

        // 이용가능 통화 리스트 (중복 제거)
        List<String> availCurrencies = totalSharedList.stream()
                .map(TotalShared::getTotalSharedCurrency)
                .distinct()
                .toList();

        return SharedDetailsResponse.builder()
                .tripId(tripId)
                .tripStartDate(tripStartDate)
                .defaultCurrency(defaultCurrency)
                .availCurrencies(availCurrencies)
                .sharedBudgetDetails(dayGroups)
                .totalSharedBudget(totalSharedBudget)
                .build();
    }
}
