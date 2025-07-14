package com.snapsplit.backend.feature.addExpense.service;

import com.snapsplit.backend.domain.expense.entity.*;
import com.snapsplit.backend.domain.expense.repository.*;
import com.snapsplit.backend.feature.addExpense.dto.AddExpenseRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddExpenseService {

    private final ExpenseRepository expenseRepository;
    private final PayRepository payRepository;
    private final SplitRepository splitRepository;


    //지출 추가
    @Transactional
    public Long addExpense(Long tripId, AddExpenseRequest request) {
        var info = request.expense();
        BigDecimal rate = info.exchangeRate();
        BigDecimal expenseAmount = info.amount();

        // split 총합 검증
        BigDecimal splitTotal = request.splitters().stream()
                .map(AddExpenseRequest.SplitterDto::splitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!splitTotal.equals(expenseAmount)) {
            throw new IllegalArgumentException("splitList의 금액 합계가 지출 금액과 일치하지 않습니다.");
        }

        // pay 총합 검증
        BigDecimal payTotal = request.payers().stream()
                .map(AddExpenseRequest.PayerDto::payerAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!payTotal.equals(expenseAmount)) {
            throw new IllegalArgumentException("payList의 결제 금액 합계가 지출 금액과 일치하지 않습니다.");
        }

        // 지출 저장
        Expense expense = expenseRepository.save(
                Expense.builder()
                        .tripId(tripId)
                        .expenseDay(info.day())
                        .expenseAmount(expenseAmount)
                        .expenseCurrency(info.currency())
                        .expenseKrw(expenseAmount.multiply(rate))
                        .category(Expense.Category.valueOf(info.category().toUpperCase()))
                        .expenseName(info.expense_name())
                        .expenseMemo(info.expense_memo())
                        .paymentMethod(Expense.PaymentMethod.valueOf(info.paymentMethod().toUpperCase()))
                        .build()
        );

        // 결제자 저장
        List<Pay> pays = request.payers().stream().map(payer ->
                Pay.builder()
                        .expenseId(expense.getId())
                        .payerId(payer.tripMemberId())
                        .payAmount(payer.payerAmount())
                        .payAmountKrw(payer.payerAmount().multiply(rate))
                        .memberType(Pay.MemberType.USER) // 기본값
                        .build()
        ).toList();
        payRepository.saveAll(pays);

        // 분담자 저장
        List<Split> splits = request.splitters().stream().map(splitter ->
                Split.builder()
                        .expenseId(expense.getId())
                        .splitterId(splitter.tripMemberId())
                        .splitAmount(splitter.splitAmount())
                        .splitAmountKrw(splitter.splitAmount().multiply(rate))
                        .build()
        ).toList();
        splitRepository.saveAll(splits);

        return expense.getId();
    }


    //지출 삭제
    @Transactional
    public void deleteExpense(Long expenseId) {
        payRepository.deleteByExpenseId(expenseId);
        splitRepository.deleteByExpenseId(expenseId);
        expenseRepository.deleteById(expenseId);
    }
}
