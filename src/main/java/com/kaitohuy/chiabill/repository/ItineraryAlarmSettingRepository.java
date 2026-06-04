package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.ItineraryAlarmSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ItineraryAlarmSettingRepository extends JpaRepository<ItineraryAlarmSetting, Long> {
    Optional<ItineraryAlarmSetting> findByUserIdAndTripId(Long userId, Long tripId);
}
