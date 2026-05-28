package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.TripHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

@Repository
public interface TripHistoryRepository extends JpaRepository<TripHistory, Long> {
    List<TripHistory> findByTripIdOrderByCreatedAtDesc(Long tripId);

    @Query("SELECT th FROM TripHistory th WHERE th.tripId = :tripId " +
           "AND (:actions IS NULL OR th.action IN :actions) " +
           "AND (cast(:startDate as timestamp) IS NULL OR th.createdAt >= :startDate) " +
           "AND (cast(:endDate as timestamp) IS NULL OR th.createdAt <= :endDate) " +
           "ORDER BY th.createdAt DESC")
    Page<TripHistory> findFilteredHistories(@Param("tripId") Long tripId,
                                            @Param("actions") List<String> actions,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate,
                                            Pageable pageable);
}
