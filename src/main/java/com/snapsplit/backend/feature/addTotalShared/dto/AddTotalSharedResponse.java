package com.snapsplit.backend.feature.addTotalShared.dto;

import com.snapsplit.backend.domain.shared.entity.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class AddTotalSharedResponse {
    private Long sharedId; // 경비 총액 아이디
    private Long tripId; // 여행 아이디
    private BigDecimal amount; // 경비 총액
    private String currency; // 통화
    private PaymentMethod paymentMethod; // 입금 방식
    private LocalDate createdAt; // 입금 일자
}
