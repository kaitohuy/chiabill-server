package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.Payment;
import com.kaitohuy.chiabill.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    
    List<Payment> findByTripIdAndStatusAndIsDeletedFalse(Long tripId, PaymentStatus status);

    List<Payment> findByTripIdInAndStatusAndIsDeletedFalse(java.util.List<Long> tripIds, PaymentStatus status);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Payment p WHERE p.trip.id IN :tripIds")
    void deleteByTripIdIn(@org.springframework.data.repository.query.Param("tripIds") java.util.List<Long> tripIds);

    List<Payment> findByTripIdAndIsDeletedFalse(Long tripId);

    Optional<Payment> findByLinkedContributionIdAndIsDeletedFalse(Long contributionId);
}
