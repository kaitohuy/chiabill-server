package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.Trip;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TripRepository extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip> {

    // 🔥 Get all trips user tham gia
    @Query("""
        SELECT t FROM Trip t
        JOIN TripMember tm ON tm.trip = t
        WHERE tm.user.id = :userId
        AND tm.isActive = true
        AND t.isDeleted = false
    """)
    List<Trip> findAllByUserId(@Param("userId") Long userId);

    // 🔥 Load trip + members
    @Query("""
        SELECT t FROM Trip t
        LEFT JOIN FETCH t.createdBy
        WHERE t.id = :tripId
    """)
    Optional<Trip> findByIdWithCreator(@Param("tripId") Long tripId);

    List<Trip> findByIsDeletedTrueAndUpdatedAtBefore(java.time.LocalDateTime threshold, org.springframework.data.domain.Pageable pageable);
}