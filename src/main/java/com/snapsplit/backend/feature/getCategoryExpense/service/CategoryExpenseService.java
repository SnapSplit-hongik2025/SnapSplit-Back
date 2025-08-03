package com.snapsplit.backend.feature.getCategoryExpense.service;

import com.snapsplit.backend.domain.expense.entity.CategoryExpense;
import com.snapsplit.backend.domain.expense.entity.Expense;
import com.snapsplit.backend.domain.expense.repository.CategoryExpenseRepository;
import com.snapsplit.backend.domain.trip.entity.Trip;
import com.snapsplit.backend.domain.trip.repository.TripRepository;
import com.snapsplit.backend.feature.getCategoryExpense.dto.CategoryExpenseResponse;
import com.snapsplit.backend.feature.getCategoryExpense.dto.CategoryExpenseResponse.CategoryExpenseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryExpenseService {

    private final TripRepository tripRepository;
    private final CategoryExpenseRepository categoryExpenseRepository;

    @Transactional(readOnly = true)
    public CategoryExpenseResponse getCategoryStatistics(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

        List<CategoryExpense> categoryExpenses = categoryExpenseRepository.findByTrip(trip);

        Map<Expense.Category, BigDecimal> amountMap = categoryExpenses.stream()
                .collect(Collectors.toMap(
                        CategoryExpense::getCategory,
                        CategoryExpense::getAmountKRW
                ));

        List<CategoryExpenseDto> categoryExpenseDtos = Arrays.stream(Expense.Category.values())
                .map(cat -> new CategoryExpenseDto(cat, amountMap.getOrDefault(cat, BigDecimal.ZERO)))
                .toList();

        BigDecimal total = categoryExpenseDtos.stream()
                .map(CategoryExpenseDto::amountKRW)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CategoryExpenseResponse(total, categoryExpenseDtos);
    }


    //지출 추가시 카테고리별 누적 지출 금액 증가
    @Transactional
    public void updateOnExpenseAdd(Long tripId, Expense.Category category, BigDecimal amountKRW) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

        CategoryExpense categoryExpense = categoryExpenseRepository.findByTripAndCategory(trip, category)
                .orElse(null);

        if (categoryExpense == null) {
            categoryExpense = CategoryExpense.builder()
                    .trip(trip)
                    .category(category)
                    .amountKRW(amountKRW)
                    .build();
        } else {
            categoryExpense.increase(amountKRW);
        }

        categoryExpenseRepository.save(categoryExpense);
    }

    // 지출 삭제시 카테고리별 누적 지출 금액 차감
    @Transactional
    public void updateOnExpenseDelete(Long tripId, Expense.Category category, BigDecimal amountKRW) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행이 존재하지 않습니다."));

        CategoryExpense categoryExpense = categoryExpenseRepository.findByTripAndCategory(trip, category)
                .orElse(null);

        if (categoryExpense != null) {
            categoryExpense.decrease(amountKRW);
            categoryExpenseRepository.save(categoryExpense);
        }
    }
}
