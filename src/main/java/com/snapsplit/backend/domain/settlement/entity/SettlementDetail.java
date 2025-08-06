package com.snapsplit.backend.domain.settlement.entity;

import com.snapsplit.backend.domain.tripmember.entity.TripMember;
import com.snapsplit.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "settlement_detail")
public class SettlementDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 정산 세부내역 아이디

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement; // 정산 아이디

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private TripMember sender; // 보낼 TripMember

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private TripMember receiver; // 받을 TripMember

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount; // 보낼 금액
}
