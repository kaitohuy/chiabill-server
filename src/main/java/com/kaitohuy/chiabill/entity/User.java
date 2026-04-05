package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    private String avatarUrl;

    private String bankQrUrl;

    private String bankId;

    private String accountNo;

    private String provider;

    private String providerId;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isAnonymous = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isGhost = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "integer default 1")
    private Integer paymentPriority = 1;

    @Column(unique = true)
    private String phone;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean allowAutoAdd = true;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean allowAutoApprovePayment = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_by")
    private User managedBy;
}