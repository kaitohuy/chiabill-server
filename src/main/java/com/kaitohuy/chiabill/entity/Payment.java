package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_trip", columnList = "trip_id"),
    @Index(name = "idx_payment_from", columnList = "from_user_id"),
    @Index(name = "idx_payment_to", columnList = "to_user_id"),
    @Index(name = "idx_payment_is_deleted", columnList = "is_deleted")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    // Người thực hiện thanh toán hộ (null = tự trả, not null = người được trả hộ)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "on_behalf_of_user_id")
    private User onBehalfOfUser;

    @Column(nullable = false)
    private BigDecimal amount;

    private String proofUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * Link tới GroupFundContribution tạo ra payment này (nếu đây là payment xác nhận nộp quỹ).
     * Dùng để khi reverse contribution, có thể soft-delete payment tương ứng.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_contribution_id", nullable = true)
    private GroupFundContribution linkedContribution;
}
