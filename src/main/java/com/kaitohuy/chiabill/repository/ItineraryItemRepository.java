package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.ItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {

    @Query("SELECT it FROM ItineraryItem it WHERE it.trip.id = :tripId AND it.isDeleted = false ORDER BY it.dayNumber ASC, it.timeRange ASC")
    List<ItineraryItem> findActiveItineraryByTripId(@Param("tripId") Long tripId);

    @Modifying
    @Query("DELETE FROM ItineraryItem it WHERE it.trip.id = :tripId")
    void hardDeleteByTripId(@Param("tripId") Long tripId);
}
