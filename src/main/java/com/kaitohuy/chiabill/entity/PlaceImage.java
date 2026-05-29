package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "place_images", indexes = {
    @Index(name = "idx_place_image_is_deleted", columnList = "is_deleted"),
    @Index(name = "idx_place_image_place_id", columnList = "place_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(nullable = false, columnDefinition = "varchar(255) default 'Khác'")
    private String album; // "Phong cảnh", "Check-in", "Ẩm thực", "Khác"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;
}
