package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.entity.*;
import com.kaitohuy.chiabill.repository.*;
import com.kaitohuy.chiabill.service.interfaces.ItineraryNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryNotificationServiceImpl implements ItineraryNotificationService {

    private final ItineraryAlarmSettingRepository settingRepository;
    private final ScheduledNotificationRepository scheduledNotificationRepository;
    private final TripMemberRepository memberRepository;
    private final ItineraryItemRepository itineraryItemRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;

    @Override
    @Transactional(readOnly = true)
    public ItineraryAlarmSetting getSettings(Long tripId, Long userId) {
        return settingRepository.findByUserIdAndTripId(userId, tripId)
                .orElseGet(() -> ItineraryAlarmSetting.builder()
                        .user(userRepository.getReferenceById(userId))
                        .trip(tripRepository.getReferenceById(tripId))
                        .alarmEnabled(true)
                        .alarmValue(15)
                        .alarmUnit("Phút")
                        .build());
    }

    @Override
    @Transactional
    public ItineraryAlarmSetting saveSettings(Long tripId, Long userId, Boolean alarmEnabled, Integer alarmValue, String alarmUnit) {
        ItineraryAlarmSetting setting = settingRepository.findByUserIdAndTripId(userId, tripId)
                .orElseGet(() -> ItineraryAlarmSetting.builder()
                        .user(userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found")))
                        .trip(tripRepository.findById(tripId).orElseThrow(() -> new RuntimeException("Trip not found")))
                        .build());

        setting.setAlarmEnabled(alarmEnabled != null ? alarmEnabled : true);
        setting.setAlarmValue(alarmValue != null ? alarmValue : 15);
        setting.setAlarmUnit(alarmUnit != null ? alarmUnit : "Phút");

        ItineraryAlarmSetting saved = settingRepository.save(setting);
        log.info("Saved itinerary settings for user {} in trip {}: enabled={}, value={}, unit={}", 
                userId, tripId, saved.getAlarmEnabled(), saved.getAlarmValue(), saved.getAlarmUnit());

        // Reschedule alarms for this member
        rescheduleAlarmsForMember(tripId, userId);

        return saved;
    }

    @Override
    @Transactional
    public void rescheduleAlarmsForTrip(Long tripId) {
        List<TripMember> activeMembers = memberRepository.findActiveMembersWithUser(tripId);
        log.info("Rescheduling itinerary alarms for all {} active members of trip {}", activeMembers.size(), tripId);
        for (TripMember member : activeMembers) {
            rescheduleAlarmsForMember(tripId, member.getUser().getId());
        }
    }

    @Override
    @Transactional
    public void rescheduleAlarmsForMember(Long tripId, Long userId) {
        // 1. Delete existing unsent notifications for this member and trip
        scheduledNotificationRepository.deleteUnsentByTripIdAndUserId(tripId, userId);

        // 2. Load settings
        ItineraryAlarmSetting settings = getSettings(tripId, userId);
        if (settings.getId() == null) {
            // Save the default settings so it's persisted
            settingRepository.save(settings);
        }

        if (Boolean.FALSE.equals(settings.getAlarmEnabled())) {
            log.info("Alarms disabled for user {} in trip {}. Skipping scheduling.", userId, tripId);
            return;
        }

        // 3. Load active itinerary items
        List<ItineraryItem> items = itineraryItemRepository.findActiveItineraryByTripId(tripId);
        if (items.isEmpty()) {
            return;
        }

        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) return;

        LocalDateTime baseDate = trip.getStartDate() != null ? trip.getStartDate() : LocalDateTime.now();

        // 4. Generate scheduled notifications
        for (ItineraryItem item : items) {
            LocalDateTime scheduledTime = calculateScheduledTime(baseDate, item.getDayNumber(), item.getTimeRange(), 
                    settings.getAlarmValue(), settings.getAlarmUnit());

            if (scheduledTime == null) continue;

            // Only schedule if it's in the future
            if (scheduledTime.isAfter(LocalDateTime.now())) {
                ScheduledNotification notification = ScheduledNotification.builder()
                        .user(settings.getUser())
                        .trip(trip)
                        .itineraryItem(item)
                        .scheduledTime(scheduledTime)
                        .title("Sắp đến lịch trình: " + item.getActivity())
                        .message("Thời gian diễn ra: " + item.getTimeRange() + " (Báo trước " + settings.getAlarmValue() + " " + settings.getAlarmUnit() + ")")
                        .isSent(false)
                        .build();

                scheduledNotificationRepository.save(notification);
                log.info("Scheduled alarm for user {} in trip {} for item '{}' at {}", 
                        userId, tripId, item.getActivity(), scheduledTime);
            }
        }
    }

    @Override
    @Transactional
    public void rescheduleAlarmsForItem(Long tripId, Long itemId) {
        scheduledNotificationRepository.deleteUnsentByItineraryItemId(itemId);

        ItineraryItem item = itineraryItemRepository.findById(itemId).orElse(null);
        if (item == null || Boolean.TRUE.equals(item.getIsDeleted())) return;

        Trip trip = item.getTrip();
        LocalDateTime baseDate = trip.getStartDate() != null ? trip.getStartDate() : LocalDateTime.now();

        List<TripMember> activeMembers = memberRepository.findActiveMembersWithUser(tripId);
        for (TripMember member : activeMembers) {
            Long userId = member.getUser().getId();
            ItineraryAlarmSetting settings = getSettings(tripId, userId);

            if (Boolean.TRUE.equals(settings.getAlarmEnabled())) {
                LocalDateTime scheduledTime = calculateScheduledTime(baseDate, item.getDayNumber(), item.getTimeRange(), 
                        settings.getAlarmValue(), settings.getAlarmUnit());

                if (scheduledTime != null && scheduledTime.isAfter(LocalDateTime.now())) {
                    ScheduledNotification notification = ScheduledNotification.builder()
                            .user(member.getUser())
                            .trip(trip)
                            .itineraryItem(item)
                            .scheduledTime(scheduledTime)
                            .title("Sắp đến lịch trình: " + item.getActivity())
                            .message("Thời gian diễn ra: " + item.getTimeRange() + " (Báo trước " + settings.getAlarmValue() + " " + settings.getAlarmUnit() + ")")
                            .isSent(false)
                            .build();

                    scheduledNotificationRepository.save(notification);
                    log.info("Scheduled alarm for user {} for new/updated item '{}' at {}", 
                            userId, item.getActivity(), scheduledTime);
                }
            }
        }
    }

    @Override
    @Transactional
    public void removeAlarmsForMember(Long tripId, Long userId) {
        scheduledNotificationRepository.deleteUnsentByTripIdAndUserId(tripId, userId);
        log.info("Removed unsent notifications for user {} in trip {}", userId, tripId);
    }

    private LocalDateTime calculateScheduledTime(LocalDateTime tripStartDate, int dayNumber, String timeRange, int alarmValue, String alarmUnit) {
        if (tripStartDate == null || timeRange == null || timeRange.trim().isEmpty()) {
            return null;
        }
        String[] parts = timeRange.split("-");
        if (parts.length == 0) return null;
        String startTimeStr = parts[0].trim();
        String[] timeParts = startTimeStr.split(":");
        if (timeParts.length < 2) return null;
        try {
            int hour = Integer.parseInt(timeParts[0].trim());
            int minute = Integer.parseInt(timeParts[1].trim());
            int offsetDays = (dayNumber > 0) ? (dayNumber - 1) : 0;
            
            LocalDateTime activityDateTime = LocalDateTime.of(
                    tripStartDate.getYear(),
                    tripStartDate.getMonthValue(),
                    tripStartDate.getDayOfMonth(),
                    hour,
                    minute,
                    0,
                    0
            ).plusDays(offsetDays);
            
            switch (alarmUnit) {
                case "Giây":
                    return activityDateTime.minusSeconds(alarmValue);
                case "Giờ":
                    return activityDateTime.minusHours(alarmValue);
                case "Ngày":
                    return activityDateTime.minusDays(alarmValue);
                case "Phút":
                default:
                    return activityDateTime.minusMinutes(alarmValue);
            }
        } catch (Exception e) {
            log.warn("Failed to parse timeRange or calculate scheduled time: timeRange={}, error={}", timeRange, e.getMessage());
            return null;
        }
    }
}
