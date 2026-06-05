package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_feedback")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}
