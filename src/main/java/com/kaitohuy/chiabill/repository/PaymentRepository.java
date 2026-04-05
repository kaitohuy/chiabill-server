package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.Payment;
import com.kaitohuy.chiabill.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    
    List<Payment> findByTripIdAndStatus(Long tripId, PaymentStatus status);

    List<Payment> findByTripId(Long tripId);
}
