package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.Payment;
import com.kaitohuy.chiabill.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    
    List<Payment> findByTripIdAndStatus(Long tripId, PaymentStatus status);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Payment p WHERE p.trip.id IN :tripIds")
    void deleteByTripIdIn(@org.springframework.data.repository.query.Param("tripIds") java.util.List<Long> tripIds);

    List<Payment> findByTripId(Long tripId);
}
