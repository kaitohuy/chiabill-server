package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.TripInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TripInvitationRepository extends JpaRepository<TripInvitation, String> {

    @Query("SELECT i FROM TripInvitation i JOIN FETCH i.trip JOIN FETCH i.createdBy WHERE i.id = :id AND i.isActive = true")
    Optional<TripInvitation> findByIdAndIsActiveTrue(String id);

    Optional<TripInvitation> findFirstByTripIdAndIsActiveTrueOrderByCreatedAtDesc(Long tripId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM TripInvitation ti WHERE ti.trip.id IN :tripIds")
    void deleteByTripIdIn(@org.springframework.data.repository.query.Param("tripIds") java.util.List<Long> tripIds);
}
