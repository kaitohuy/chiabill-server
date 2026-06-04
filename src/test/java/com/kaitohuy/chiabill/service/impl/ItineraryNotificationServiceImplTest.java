package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.entity.*;
import com.kaitohuy.chiabill.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItineraryNotificationServiceImplTest {

    @Mock
    private ItineraryAlarmSettingRepository settingRepository;
    @Mock
    private ScheduledNotificationRepository scheduledNotificationRepository;
    @Mock
    private TripMemberRepository memberRepository;
    @Mock
    private ItineraryItemRepository itineraryItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private ItineraryNotificationServiceImpl service;

    private User user;
    private Trip trip;
    private ItineraryAlarmSetting setting;
    private ItineraryItem item;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        trip = new Trip();
        trip.setId(1L);
        trip.setStartDate(LocalDateTime.of(2026, 6, 10, 10, 0, 0));

        setting = ItineraryAlarmSetting.builder()
                .id(1L)
                .user(user)
                .trip(trip)
                .alarmEnabled(true)
                .alarmValue(15)
                .alarmUnit("Phút")
                .build();

        item = ItineraryItem.builder()
                .id(1L)
                .trip(trip)
                .dayNumber(2) // Day 2 -> starts on 2026-06-11
                .timeRange("14:30 - 15:30")
                .activity("Sightseeing at Temple")
                .build();
    }

    @Test
    void rescheduleAlarmsForMember_CalculatesScheduledTimeCorrectly() {
        // GIVEN
        when(settingRepository.findByUserIdAndTripId(1L, 1L)).thenReturn(Optional.of(setting));
        when(itineraryItemRepository.findActiveItineraryByTripId(1L)).thenReturn(Collections.singletonList(item));
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        // WHEN
        service.rescheduleAlarmsForMember(1L, 1L);

        // THEN
        verify(scheduledNotificationRepository).deleteUnsentByTripIdAndUserId(1L, 1L);
        
        ArgumentCaptor<ScheduledNotification> captor = ArgumentCaptor.forClass(ScheduledNotification.class);
        verify(scheduledNotificationRepository).save(captor.capture());
        
        ScheduledNotification savedNotification = captor.getValue();
        assertNotNull(savedNotification);
        assertEquals("Sắp đến lịch trình: Sightseeing at Temple", savedNotification.getTitle());
        
        // Expected Time calculation:
        // Start date: 2026-06-10 10:00:00
        // Day number 2: 2026-06-11
        // Start time: 14:30
        // Alarm value: 15 Phút before -> 2026-06-11 14:15:00
        LocalDateTime expectedTime = LocalDateTime.of(2026, 6, 11, 14, 15, 0);
        assertEquals(expectedTime, savedNotification.getScheduledTime());
        assertFalse(savedNotification.getIsSent());
    }

    @Test
    void rescheduleAlarmsForMember_WhenAlarmsDisabled_DoesNotSave() {
        // GIVEN
        setting.setAlarmEnabled(false);
        when(settingRepository.findByUserIdAndTripId(1L, 1L)).thenReturn(Optional.of(setting));

        // WHEN
        service.rescheduleAlarmsForMember(1L, 1L);

        // THEN
        verify(scheduledNotificationRepository).deleteUnsentByTripIdAndUserId(1L, 1L);
        verify(scheduledNotificationRepository, never()).save(any(ScheduledNotification.class));
    }
}
