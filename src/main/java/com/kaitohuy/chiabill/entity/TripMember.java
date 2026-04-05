package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_members", uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "user_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    private String role = "MEMBER";

    @Column(nullable = false, columnDefinition = "varchar(255) default 'ACTIVE'")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();
}