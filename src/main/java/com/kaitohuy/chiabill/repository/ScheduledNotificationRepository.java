package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.ScheduledNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledNotificationRepository extends JpaRepository<ScheduledNotification, Long> {

    List<ScheduledNotification> findByScheduledTimeBeforeAndIsSentFalseAndIsDeletedFalse(LocalDateTime time);

    @Modifying
    @Query("UPDATE ScheduledNotification sn SET sn.isDeleted = true WHERE sn.trip.id = :tripId AND sn.isSent = false")
    void deleteUnsentByTripId(@Param("tripId") Long tripId);

    @Modifying
    @Query("UPDATE ScheduledNotification sn SET sn.isDeleted = true WHERE sn.trip.id = :tripId AND sn.user.id = :userId AND sn.isSent = false")
    void deleteUnsentByTripIdAndUserId(@Param("tripId") Long tripId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ScheduledNotification sn SET sn.isDeleted = true WHERE sn.itineraryItem.id = :itemId AND sn.isSent = false")
    void deleteUnsentByItineraryItemId(@Param("itemId") Long itemId);

    @Modifying
    @Query("DELETE FROM ScheduledNotification sn WHERE sn.isSent = true OR sn.isDeleted = true")
    void cleanUpSentOrDeleted();
}
