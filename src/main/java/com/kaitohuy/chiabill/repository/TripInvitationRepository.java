package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.TripInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TripInvitationRepository extends JpaRepository<TripInvitation, String> {

    @Query("SELECT i FROM TripInvitation i JOIN FETCH i.trip JOIN FETCH i.createdBy WHERE i.id = :id AND i.isActive = true")
    Optional<TripInvitation> findByIdAndIsActiveTrue(String id);

    Optional<TripInvitation> findFirstByTripIdAndIsActiveTrueOrderByCreatedAtDesc(Long tripId);
}
