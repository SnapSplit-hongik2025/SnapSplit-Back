package com.snapsplit.backend.domain.receipt.entity;

import com.snapsplit.backend.domain.expense.entity.Expense;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "receipt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false, unique = true)
    private Expense expense;

    @Column(name = "receipt_url", length = 2048, nullable = false)
    private String receiptUrl;

    /**
     * 영수증 파싱 결과(JSON 그대로 저장)
     * MySQL에서는 JSON 타입으로, Hibernate는 String으로 매핑
     */
    @Column(name = "extracted_data", columnDefinition = "json")
    private String extractedData;

}
