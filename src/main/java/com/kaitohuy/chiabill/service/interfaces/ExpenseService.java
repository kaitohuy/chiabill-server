package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.request.CreateExpenseRequest;
import com.kaitohuy.chiabill.dto.request.SearchExpenseRequest;
import com.kaitohuy.chiabill.dto.response.CategoryStatResponse;
import com.kaitohuy.chiabill.dto.response.ExpenseResponse;
import com.kaitohuy.chiabill.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ExpenseService {

    ExpenseResponse createExpense(CreateExpenseRequest request);

    List<ExpenseResponse> getExpensesByTrip(Long tripId, Long userId);

    PageResponse<ExpenseResponse> searchExpenses(Long tripId, Long userId, SearchExpenseRequest request, Pageable pageable);

    ExpenseResponse updateExpense(Long expenseId, Long userId, com.kaitohuy.chiabill.dto.request.UpdateExpenseRequest request);

    void deleteExpense(Long expenseId, Long userId);

    List<CategoryStatResponse> getExpenseStats(Long tripId, Long userId);
}