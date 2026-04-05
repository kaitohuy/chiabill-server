package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.Expense;
import com.kaitohuy.chiabill.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

    List<ExpenseSplit> findByExpenseId(Long expenseId);

    List<ExpenseSplit> findByUserId(Long userId);

    List<ExpenseSplit> findByExpenseTripId(Long tripId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM ExpenseSplit e WHERE e.expense.id = :expenseId")
    void deleteByExpenseId(Long expenseId);
}