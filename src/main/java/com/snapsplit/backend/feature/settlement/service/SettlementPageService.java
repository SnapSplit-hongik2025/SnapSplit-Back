package com.snapsplit.backend.feature.settlement.service;

import com.snapsplit.backend.domain.expense.entity.Expense;
import com.snapsplit.backend.domain.expense.repository.ExpenseRepository;
import com.snapsplit.backend.domain.settlement.repository.SettlementRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.feature.settlement.dto.SettlementPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementPageService {

    private final TripRepository tripRepository;
    private final SettlementRepository settlementRepository;
    private final ExpenseRepository expenseRepository;

    public SettlementPageResponse getSettlementPage(Long tripId) {

        // 여행 찾기
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("여행이 존재하지 않습니다."));

        // 정산 내역 정보
        List<SettlementPageResponse.SettlementSummary> completeSettlement = settlementRepository.findAllByTripId(tripId).stream()
                .map(settlement -> SettlementPageResponse.SettlementSummary.builder()
                        .id(settlement.getId())
                        .startDate(settlement.getStartDate())
                        .endDate(settlement.getEndDate())
                        .build())
                .toList();

        // 여행 기간 내 모든 지출 날짜 세트화
        LocalDate start = trip.getStartDate().minusDays(1);
        LocalDate end = trip.getEndDate();

        List<Expense> expensesInRange =
                expenseRepository.findByTripIdAndExpenseDateBetween(tripId, start, end);

        Set<LocalDate> expenseDates = expensesInRange.stream()
                .map(Expense::getExpenseDate)
                .collect(Collectors.toSet());

        // 날짜별 지출 유무 리스트 생성
        List<SettlementPageResponse.DailyExpenseStatus> dailyExpenseStatus = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            dailyExpenseStatus.add(
                    SettlementPageResponse.DailyExpenseStatus.builder()
                            .date(d)
                            .hasExpense(expenseDates.contains(d))
                            .build()
            );
        }

        // 여행 시작일 + 정산 내역 정보 return
        return SettlementPageResponse.builder()
                .trip(SettlementPageResponse.TripInfo.builder()
                        .startDate(trip.getStartDate())
                        .endDate(trip.getEndDate())
                        .build())
                .completeSettlement(completeSettlement)
                .dailyExpenseStatus(dailyExpenseStatus)
                .build();
    }
}
