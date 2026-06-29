package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "places", indexes = {
    @Index(name = "idx_place_is_deleted", columnList = "is_deleted"),
    @Index(name = "idx_place_category", columnList = "category"),
    @Index(name = "idx_place_city", columnList = "city")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Place extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 255)
    private String nameEn;

    @Column(length = 50)
    private String category; // BEACH, MOUNTAIN, AMUSEMENT_PARK, MALL, etc.

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private String city;

    @Column(length = 255)
    private String cityEn;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String summaryEn;

    @Column(columnDefinition = "TEXT")
    private String ticketPrices;

    @Column(columnDefinition = "TEXT")
    private String ticketPricesEn;

    @Column(length = 100)
    private String openingHours;

    @Column(length = 100)
    private String openingHoursEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id") // Null if seeded by system, otherwise the user who created it
    private User creator;
}
