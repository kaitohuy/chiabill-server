package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.TripMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TripMemberRepository extends JpaRepository<TripMember, Long> {

    List<TripMember> findByTripId(Long tripId);

    List<TripMember> findByTripIdAndIsActiveTrue(Long tripId);

    Optional<TripMember> findByTripIdAndUserId(Long tripId, Long userId);

    boolean existsByTripIdAndUserId(Long tripId, Long userId);

    List<TripMember> findByUserIdAndIsActiveTrue(Long userId);

    @Query("""
    SELECT tm FROM TripMember tm
    JOIN FETCH tm.user
    WHERE tm.trip.id = :tripId AND tm.isActive = true
""")
    List<TripMember> findActiveMembersWithUser(Long tripId);

    @Query("""
    SELECT tm FROM TripMember tm
    JOIN FETCH tm.user
    WHERE tm.trip.id IN :tripIds
    AND tm.isActive = true
""")
    List<TripMember> findAllByTripIdsWithUser(List<Long> tripIds);

    @Query("""
    SELECT tm FROM TripMember tm
    JOIN FETCH tm.user
    WHERE tm.trip.id = :tripId AND tm.isActive = true AND tm.status = com.kaitohuy.chiabill.entity.MemberStatus.ACTIVE
""")
    List<TripMember> findEnabledMembersWithUser(@org.springframework.data.repository.query.Param("tripId") Long tripId);

    boolean existsByTripIdAndUserIdAndRole(Long tripId, Long userId, String role);
}