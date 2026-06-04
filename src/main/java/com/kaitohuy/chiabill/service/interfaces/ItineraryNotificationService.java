package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.entity.ItineraryAlarmSetting;

public interface ItineraryNotificationService {

    ItineraryAlarmSetting getSettings(Long tripId, Long userId);

    ItineraryAlarmSetting saveSettings(Long tripId, Long userId, Boolean alarmEnabled, Integer alarmValue, String alarmUnit);

    void rescheduleAlarmsForTrip(Long tripId);

    void rescheduleAlarmsForMember(Long tripId, Long userId);

    void rescheduleAlarmsForItem(Long tripId, Long itemId);

    void removeAlarmsForMember(Long tripId, Long userId);
}
