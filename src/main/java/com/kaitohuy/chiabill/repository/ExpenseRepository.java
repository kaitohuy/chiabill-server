package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.Expense;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    // 🔥 Basic
    List<Expense> findByTripIdAndIsDeletedFalse(Long tripId);

    java.util.Optional<Expense> findByClientUuid(String clientUuid);

    // 🔥 Find by payer (for ghost claim)
    List<Expense> findByPayerIdAndIsDeletedFalse(Long payerId);

    // 🔥 Load full expense (payer + category + splits) — dùng cho Export Excel/PDF
    @Query("""
        SELECT DISTINCT e FROM Expense e
        LEFT JOIN FETCH e.payer
        LEFT JOIN FETCH e.category
        LEFT JOIN FETCH e.splits s
        LEFT JOIN FETCH s.user
        WHERE e.trip.id = :tripId
        AND e.isDeleted = false
        ORDER BY e.expenseDate ASC, e.id ASC
    """)
    List<Expense> findAllByTripIdWithPayerAndCategory(@Param("tripId") Long tripId);

    // 🔥 Load full expense (payer + splits)
    @Query("""
        SELECT DISTINCT e FROM Expense e
        LEFT JOIN FETCH e.payer
        LEFT JOIN FETCH e.trip
        WHERE e.trip.id = :tripId
        AND e.isDeleted = false
        ORDER BY e.expenseDate DESC, e.id DESC
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
        ORDER BY e.expenseDate DESC, e.id DESC
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
        ORDER BY e.expenseDate DESC, e.id DESC
    """)
    List<Expense> fetchAllDataForSettlement(@Param("tripId") Long tripId);

    @Query("""
        SELECT DISTINCT e FROM Expense e
        LEFT JOIN FETCH e.payer
        LEFT JOIN FETCH e.trip
        LEFT JOIN FETCH e.splits s
        LEFT JOIN FETCH s.user
        WHERE e.trip.id IN :tripIds
        AND e.isDeleted = false
        ORDER BY e.expenseDate DESC, e.id DESC
    """)
    List<Expense> fetchAllDataForSettlementIn(@Param("tripIds") List<Long> tripIds);

    @Query("""
        SELECT new com.kaitohuy.chiabill.dto.response.TripStatResponse(t.id, t.name, SUM(es.amount), t.categoryIcon)
        FROM ExpenseSplit es
        JOIN es.expense e
        JOIN e.trip t
        JOIN TripMember tm ON tm.trip = t AND tm.user.id = :userId
        WHERE es.user.id = :userId
        AND tm.isActive = true
        AND e.isDeleted = false
        AND t.isDeleted = false
        AND (e.groupFund IS NULL OR e.isFromFund = true)
        AND (:month IS NULL OR MONTH(e.expenseDate) = :month)
        AND (:year IS NULL OR YEAR(e.expenseDate) = :year)
        GROUP BY t.id, t.name, t.categoryIcon
        ORDER BY SUM(es.amount) DESC
    """)
    List<com.kaitohuy.chiabill.dto.response.TripStatResponse> getOverallExpenseStats(
            @Param("userId") Long userId,
            @Param("month") Integer month,
            @Param("year") Integer year);

    @Query("""
        SELECT new com.kaitohuy.chiabill.dto.response.CategoryStatResponse(c.id, c.name, c.icon, SUM(e.totalAmount))
        FROM Expense e
        JOIN e.category c
        WHERE e.trip.id = :tripId
        AND e.isDeleted = false
        AND (e.groupFund IS NULL OR e.isFromFund = true)
        GROUP BY c.id, c.name, c.icon
        ORDER BY SUM(e.totalAmount) DESC
    """)
    List<com.kaitohuy.chiabill.dto.response.CategoryStatResponse> getExpenseStatsByCategory(@Param("tripId") Long tripId);

    @Modifying
    @Query("DELETE FROM Expense e WHERE e.trip.id IN :tripIds")
    void deleteByTripIdIn(@Param("tripIds") List<Long> tripIds);

    @Query("SELECT e.receiptUrl FROM Expense e WHERE e.trip.id IN :tripIds AND e.receiptUrl IS NOT NULL")
    java.util.List<String> findReceiptUrlsByTripIdIn(@Param("tripIds") java.util.List<Long> tripIds);

    @Query("SELECT e.receiptUrl FROM Expense e WHERE e.isDeleted = true AND e.updatedAt < :threshold AND e.receiptUrl IS NOT NULL")
    java.util.List<String> findReceiptUrlsByIsDeletedTrueAndUpdatedAtBefore(@Param("threshold") java.time.LocalDateTime threshold);

    @Modifying
    @Query("DELETE FROM Expense e WHERE e.isDeleted = true AND e.updatedAt < :threshold")
    void deleteSoftDeletedExpenses(@Param("threshold") java.time.LocalDateTime threshold);

    @Query("""
        SELECT DISTINCT e FROM Expense e
        LEFT JOIN FETCH e.payer
        LEFT JOIN FETCH e.category
        LEFT JOIN FETCH e.splits s
        LEFT JOIN FETCH s.user
        WHERE e.id IN :ids
    """)
    List<Expense> findAllByIdInWithPayerAndCategoryAndSplits(@Param("ids") List<Long> ids);

    @Query("""
        SELECT e.exchangeRate FROM Expense e
        WHERE e.currency = :currency
        AND e.exchangeRate IS NOT NULL
        ORDER BY e.createdAt DESC
        LIMIT 1
    """)
    java.math.BigDecimal findLatestExchangeRateByCurrency(@Param("currency") String currency);
}