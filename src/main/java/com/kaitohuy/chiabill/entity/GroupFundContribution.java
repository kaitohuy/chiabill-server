package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_fund_contributions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupFundContribution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_id", nullable = false)
    private GroupFund groupFund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User contributor;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime contributionDate;

    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContributionType type;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isConfirmed = false; // Đối với đợt thu bắt buộc, Thủ quỹ xác nhận đã nộp hay chưa
}
