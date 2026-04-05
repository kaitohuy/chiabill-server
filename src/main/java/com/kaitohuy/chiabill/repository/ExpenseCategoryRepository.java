package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    @Query("""
        SELECT ec FROM ExpenseCategory ec
        WHERE ec.trip.id = :tripId OR ec.trip IS NULL
        AND ec.isDeleted = false
    """)
    List<ExpenseCategory> findAllByTripIdOrSystem(@Param("tripId") Long tripId);

    List<ExpenseCategory> findByTripIdIsNull();
}
