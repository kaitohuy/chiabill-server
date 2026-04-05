package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.Expense;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    // 🔥 Basic
    List<Expense> findByTripIdAndIsDeletedFalse(Long tripId);

    // 🔥 Find by payer (for ghost claim)
    List<Expense> findByPayerIdAndIsDeletedFalse(Long payerId);

    // 🔥 Load full expense (payer + splits)
    @Query("""
        SELECT DISTINCT e FROM Expense e
        LEFT JOIN FETCH e.payer
        LEFT JOIN FETCH e.trip
        WHERE e.trip.id = :tripId
        AND e.isDeleted = false
    """)
    List<Expense> findAllByTripIdWithPayer(@Param("tripId") Long tripId);

    // 🔥 Full data để tính settlement
    @Query("""
        SELECT DISTINCT e FROM Expense e
        LEFT JOIN FETCH e.payer
        LEFT JOIN FETCH e.trip
        LEFT JOIN FETCH e.splits
        WHERE e.trip.id = :tripId
        AND e.isDeleted = false
    """)
    List<Expense> findAllForSettlement(@Param("tripId") Long tripId);

    @Query("""
        SELECT DISTINCT e FROM Expense e
        LEFT JOIN FETCH e.payer
        LEFT JOIN FETCH e.trip
        LEFT JOIN FETCH e.splits s
        LEFT JOIN FETCH s.user
        WHERE e.trip.id = :tripId
        AND e.isDeleted = false
    """)
    List<Expense> fetchAllDataForSettlement(@Param("tripId") Long tripId);

    @Query("""
        SELECT new com.kaitohuy.chiabill.dto.response.CategoryStatResponse(c.id, c.name, c.icon, SUM(e.totalAmount))
        FROM Expense e
        JOIN e.category c
        WHERE e.trip.id = :tripId
        AND e.isDeleted = false
        GROUP BY c.id, c.name, c.icon
        ORDER BY SUM(e.totalAmount) DESC
    """)
    List<com.kaitohuy.chiabill.dto.response.CategoryStatResponse> getExpenseStatsByCategory(@Param("tripId") Long tripId);
}