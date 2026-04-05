package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.CreateExpenseRequest;
import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.ExpenseResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.ExpenseService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ApiResponse<ExpenseResponse> createExpense(@RequestBody CreateExpenseRequest request) {

        return ApiResponse.<ExpenseResponse>builder()
                .success(true)
                .data(expenseService.createExpense(request))
                .build();
    }

    @GetMapping("/trip/{tripId}")
    public ApiResponse<List<ExpenseResponse>> getByTrip(
            @PathVariable Long tripId,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<List<ExpenseResponse>>builder()
                .success(true)
                .data(expenseService.getExpensesByTrip(tripId, userId))
                .build();
    }

    @GetMapping("/trip/{tripId}/search")
    public ApiResponse<com.kaitohuy.chiabill.dto.response.PageResponse<ExpenseResponse>> searchExpenses(
            @PathVariable Long tripId,
            com.kaitohuy.chiabill.dto.request.SearchExpenseRequest request,
            org.springframework.data.domain.Pageable pageable,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<com.kaitohuy.chiabill.dto.response.PageResponse<ExpenseResponse>>builder()
                .success(true)
                .data(expenseService.searchExpenses(tripId, userId, request, pageable))
                .build();
    }

    @PutMapping("/{expenseId}")
    public ApiResponse<ExpenseResponse> updateExpense(
            @PathVariable Long expenseId,
            @RequestBody com.kaitohuy.chiabill.dto.request.UpdateExpenseRequest request,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<ExpenseResponse>builder()
                .success(true)
                .data(expenseService.updateExpense(expenseId, userId, request))
                .build();
    }

    @DeleteMapping("/{expenseId}")
    public ApiResponse<?> deleteExpense(
            @PathVariable Long expenseId,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        expenseService.deleteExpense(expenseId, userId);

        return ApiResponse.builder()
                .success(true)
                .message("Expense deleted")
                .build();
    }

    @GetMapping("/trip/{tripId}/stats")
    public ApiResponse<List<com.kaitohuy.chiabill.dto.response.CategoryStatResponse>> getTripStats(
            @PathVariable Long tripId,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<List<com.kaitohuy.chiabill.dto.response.CategoryStatResponse>>builder()
                .success(true)
                .data(expenseService.getExpenseStats(tripId, userId))
                .build();
    }
}