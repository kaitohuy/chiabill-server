package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.request.CreateExpenseRequest;
import com.kaitohuy.chiabill.dto.request.SearchExpenseRequest;
import com.kaitohuy.chiabill.dto.request.UpdateExpenseRequest;
import com.kaitohuy.chiabill.dto.response.*;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.ExpenseService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ApiResponse<ExpenseResponse> createExpense(
            @RequestBody CreateExpenseRequest request,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<ExpenseResponse>builder()
                .success(true)
                .data(expenseService.createExpense(userId, request))
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
    public ApiResponse<PageResponse<ExpenseResponse>> searchExpenses(
            @PathVariable Long tripId,
            SearchExpenseRequest request,
            @PageableDefault(sort = "expenseDate", direction = Direction.DESC) Pageable pageable,
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
            @RequestBody UpdateExpenseRequest request,
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
    public ApiResponse<List<CategoryStatResponse>> getTripStats(
            @PathVariable Long tripId,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<List<com.kaitohuy.chiabill.dto.response.CategoryStatResponse>>builder()
                .success(true)
                .data(expenseService.getExpenseStats(tripId, userId))
                .build();
    }

    @GetMapping("/overall-stats")
    public ApiResponse<List<TripStatResponse>> getOverallStats(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<List<com.kaitohuy.chiabill.dto.response.TripStatResponse>>builder()
                .success(true)
                .data(expenseService.getOverallExpenseStats(userId, month, year))
                .build();
    }

    @GetMapping("/exchange-rate")
    public ApiResponse<BigDecimal> getExchangeRate(
            @RequestParam String currency) {
        
        return ApiResponse.<java.math.BigDecimal>builder()
                .success(true)
                .data(expenseService.getLatestExchangeRate(currency))
                .build();
    }

    @PostMapping(value = "/scan-receipt", consumes = MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ScanReceiptResponse> scanReceipt(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tripId") Long tripId,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<com.kaitohuy.chiabill.dto.response.ScanReceiptResponse>builder()
                .success(true)
                .data(expenseService.scanReceipt(tripId, userId, file))
                .build();
    }
}