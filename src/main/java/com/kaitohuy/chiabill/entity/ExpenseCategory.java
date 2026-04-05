package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "expense_categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String icon;

    // Nếu trip_id null -> Danh mục hệ thống dùng chung
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;
}
