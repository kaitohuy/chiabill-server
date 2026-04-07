package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "expenses", indexes = {
    @Index(name = "idx_expense_trip", columnList = "trip_id"),
    @Index(name = "idx_expense_payer", columnList = "payer_id"),
    @Index(name = "idx_expense_is_deleted", columnList = "is_deleted")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ExpenseCategory category;

    private String receiptUrl;

    private String currency;

    @Column(precision = 15, scale = 6)
    private BigDecimal exchangeRate;

    @Column(nullable = false)
    private LocalDateTime expenseDate;

    @OneToMany(mappedBy = "expense", fetch = FetchType.LAZY)
    private List<ExpenseSplit> splits;
}