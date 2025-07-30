package com.snapsplit.backend.feature.updateTotalShared.dto;

import com.snapsplit.backend.domain.shared.entity.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AddTotalSharedRequest {
    private BigDecimal amount; // 경비 입금액
    private BigDecimal exchangeRate; // 환율
    private String currency; // 경비 입금 통화
    private PaymentMethod paymentMethod; // 경비 입금방식
    private LocalDate createdAt; // 경비 입금일자
}

