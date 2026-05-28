package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.GroupFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupFundRepository extends JpaRepository<GroupFund, Long> {
    Optional<GroupFund> findByTripId(Long tripId);
    Optional<GroupFund> findByTripIdAndIsDeletedFalse(Long tripId);
}
