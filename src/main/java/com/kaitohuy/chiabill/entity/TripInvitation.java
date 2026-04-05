package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_invitations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripInvitation {

    @Id
    private String id; // UUID string as the actual invite code

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
