package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tripId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(nullable = false)
    private String action; // EDIT_EXPENSE, DELETE_EXPENSE

    @Column(columnDefinition = "TEXT")
    private String content; // Chi tiết thay đổi: "Sửa số tiền từ 10k thành 20k, ..."

    @CreationTimestamp
    private LocalDateTime createdAt;
}
