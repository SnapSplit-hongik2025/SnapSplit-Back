package com.snapsplit.backend.feature.settlement.service;

import com.snapsplit.backend.domain.expense.entity.Expense;
import com.snapsplit.backend.domain.expense.entity.Split;
import com.snapsplit.backend.domain.expense.repository.ExpenseRepository;
import com.snapsplit.backend.domain.expense.repository.SplitRepository;
import com.snapsplit.backend.domain.settlement.entity.Settlement;
import com.snapsplit.backend.domain.settlement.repository.SettlementRepository;
import com.snapsplit.backend.feature.settlement.dto.SettlementExpenseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementExpenseService {

    private final SettlementRepository settlementRepository;
    private final ExpenseRepository expenseRepository;
    private final SplitRepository splitRepository;

    public SettlementExpenseResponse getSettlementExpense(Long tripId, Long settlementId, Long memberId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("해당 정산이 존재하지 않습니다."));

        if (!settlement.getTrip().getId().equals(tripId)) {
            throw new IllegalArgumentException("정산이 해당 여행에 속하지 않습니다.");
        }

        // 정산 대상 날짜 구간 정의
        LocalDate from = settlement.getStartDate().minusDays(1); // 시작일 하루 전
        LocalDate to = settlement.getEndDate(); // 종료일

        // 정산 기간에 대한 지출들 받아오기
        List<Expense> expenses = expenseRepository.findByTripIdAndExpenseDateBetween(tripId, from, to);
        if (expenses.isEmpty()) {
            return SettlementExpenseResponse.builder()
                    .settlementDetailsByMember(List.of())
                    .totalKRW(BigDecimal.ZERO)
                    .build();
        }

        // expenses 리스트를 expenseId -> Expense 객체로 바로 찾을 수 있도록 Map 구조로 바꾸기
        Map<Long, Expense> expenseMap = expenses.stream()
                .collect(Collectors.toMap(Expense::getId, e -> e));

        List<Long> expenseIds = expenses.stream().map(Expense::getId).toList();

        // expenseId들과 memberId로 split 내역 찾기
        List<Split> splits = splitRepository.findAllByExpenseIdInAndSplitterId(expenseIds, memberId);
        if (splits.isEmpty()) {
            return SettlementExpenseResponse.builder()
                    .settlementDetailsByMember(List.of())
                    .totalKRW(BigDecimal.ZERO)
                    .build();
        }

        Map<LocalDate, List<SettlementExpenseResponse.ExpenseItem>> byDate = new HashMap<>();

        for (Split s : splits) {
            Expense ex = expenseMap.get(s.getExpenseId());
            if (ex == null) continue;

            BigDecimal amt = s.getSplitAmount();
            BigDecimal amtKrw = s.getSplitAmountKrw();

            SettlementExpenseResponse.ExpenseItem item = SettlementExpenseResponse.ExpenseItem.builder()
                    .expenseName(Optional.ofNullable(ex.getExpenseName()).orElse(""))
                    .expenseMemo(Optional.ofNullable(ex.getExpenseMemo()).orElse(""))
                    .amount(amt)
                    .amountKRW(amtKrw)
                    .expenseCurrency(ex.getExpenseCurrency())
                    .build();

            byDate.computeIfAbsent(ex.getExpenseDate(), k -> new ArrayList<>()).add(item);
        }

        List<SettlementExpenseResponse.SettlementDetailByMember> details = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> SettlementExpenseResponse.SettlementDetailByMember.builder()
                        .date(e.getKey().toString())
                        .items(e.getValue())
                        .build())
                .toList();

        BigDecimal totalKRW = byDate.values().stream()
                .flatMap(List::stream)
                .map(SettlementExpenseResponse.ExpenseItem::getAmountKRW)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SettlementExpenseResponse.builder()
                .settlementDetailsByMember(details)
                .totalKRW(totalKRW)
                .build();

    }

}
