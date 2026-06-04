package com.kaitohuy.chiabill.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "itinerary_alarm_settings", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "trip_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryAlarmSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "alarm_enabled", nullable = false)
    @Builder.Default
    private Boolean alarmEnabled = true;

    @Column(name = "alarm_value", nullable = false)
    @Builder.Default
    private Integer alarmValue = 15;

    @Column(name = "alarm_unit", nullable = false, length = 20)
    @Builder.Default
    private String alarmUnit = "Phút";
}
