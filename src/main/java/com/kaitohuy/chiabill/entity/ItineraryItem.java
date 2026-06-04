package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "itinerary_items", indexes = {
    @Index(name = "idx_itinerary_trip", columnList = "trip_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "time_range", length = 50)
    private String timeRange;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String activity;

    @Column(length = 255)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "estimated_cost", precision = 15, scale = 2)
    private BigDecimal estimatedCost;
}
