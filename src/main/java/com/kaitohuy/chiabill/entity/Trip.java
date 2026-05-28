package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "trips", indexes = {
    @Index(name = "idx_trip_is_deleted", columnList = "is_deleted")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    private String coverUrl;
    
    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "category_icon")
    private String categoryIcon;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalBudget;

    private java.time.LocalDateTime startDate;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "VND";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}