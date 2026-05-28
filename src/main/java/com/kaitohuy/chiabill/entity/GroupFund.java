package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "group_funds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupFund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false, unique = true)
    private Trip trip;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "VND";

    @Column(precision = 15, scale = 2)
    private BigDecimal alertThreshold;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treasurer_id", nullable = false)
    private User treasurer;
}
