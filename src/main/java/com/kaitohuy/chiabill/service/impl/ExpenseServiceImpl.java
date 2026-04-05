package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.request.CreateExpenseRequest;
import com.kaitohuy.chiabill.dto.request.SearchExpenseRequest;
import com.kaitohuy.chiabill.dto.request.SplitRequest;
import com.kaitohuy.chiabill.dto.request.UpdateExpenseRequest;
import com.kaitohuy.chiabill.dto.response.ExpenseResponse;
import com.kaitohuy.chiabill.dto.response.PageResponse;
import com.kaitohuy.chiabill.entity.*;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.mapper.ExpenseMapper;
import com.kaitohuy.chiabill.repository.*;
import com.kaitohuy.chiabill.repository.specification.ExpenseSpecification;
import com.kaitohuy.chiabill.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements com.kaitohuy.chiabill.service.interfaces.ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository splitRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripMemberRepository tripMemberRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final NotificationService notificationService;

    private final ExpenseMapper expenseMapper;

    @Override
    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request) {

        if (request.getTotalAmount().compareTo(new BigDecimal("9999999999999")) > 0) {
            throw new BusinessException("Số tiền quá lớn, vui lòng kiểm tra lại");
        }

        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new BusinessException("Trip not found"));

        if (Boolean.TRUE.equals(trip.getIsDeleted())) {
            throw new BusinessException("Trip has been deleted");
        }

        User payer = userRepository.findById(request.getPayerId())
                .orElseThrow(() -> new BusinessException("Payer not found"));

        validateSplits(request);

        // Collect all involved user IDs: splits + payer
        List<Long> userIds = request.getSplits().stream()
                .map(SplitRequest::getUserId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        if (userMap.size() != userIds.size()) {
            throw new BusinessException("Some users not found");
        }

        // Validate cả payer lẫn split users đều phải trong trip
        List<Long> allInvolvedIds = new ArrayList<>(userIds);
        if (!allInvolvedIds.contains(request.getPayerId())) {
            allInvolvedIds.add(request.getPayerId());
        }
        validateUsersInTrip(trip.getId(), allInvolvedIds);

        if (request.getCategoryId() == null) {
            throw new BusinessException("Vui lòng chọn danh mục chi phí");
        }

        ExpenseCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("Danh mục không tồn tại"));

        Expense expense = Expense.builder()
                .trip(trip)
                .payer(payer)
                .totalAmount(request.getTotalAmount())
                .description(request.getDescription())
                .category(category)
                .expenseDate(request.getExpenseDate())
                .currency(trip.getCurrency())
                .build();

        expenseRepository.save(expense);

        List<ExpenseSplit> splits = request.getSplits().stream()
                .map(req -> ExpenseSplit.builder()
                        .expense(expense)
                        .user(userMap.get(req.getUserId()))
                        .amount(req.getAmount())
                        .build()
                )
                .toList();

        splitRepository.saveAll(splits);

        expense.setSplits(splits);

        // 🚀 Bắn thông báo cho các thành viên trong Trip (ngoại trừ payer)
        try {
            List<TripMember> activeMembers = tripMemberRepository.findActiveMembersWithUser(trip.getId());
            for (TripMember member : activeMembers) {
                if (!member.getUser().getId().equals(payer.getId())) {
                    notificationService.sendNotification(
                            member.getUser(),
                            "Khoản chi mới: " + trip.getName(),
                            payer.getName() + " vừa thêm " + expense.getTotalAmount() + " cho " + category.getName(),
                            NotificationType.EXPENSE_CREATED,
                            trip.getId()
                    );
                }
            }
        } catch (Exception e) {
            // Không để lỗi gửi thông báo làm hỏng transaction chính của Expense
        }

        return expenseMapper.toResponse(expense);
    }

    @Override
    public List<ExpenseResponse> getExpensesByTrip(Long tripId, Long userId) {

        // Verify caller is a member of the trip
        boolean isMember = tripMemberRepository.existsByTripIdAndUserId(tripId, userId);
        if (!isMember) {
            throw new BusinessException("Access denied: not a member of this trip");
        }

        List<Expense> expenses = expenseRepository.fetchAllDataForSettlement(tripId);

        return expenses.stream()
                .map(expenseMapper::toResponse)
                .toList();
    }

    @Override
    public PageResponse<ExpenseResponse> searchExpenses(Long tripId, Long userId, SearchExpenseRequest request, Pageable pageable) {
        // 1. Verify caller is a member of the trip
        boolean isMember = tripMemberRepository.existsByTripIdAndUserId(tripId, userId);
        if (!isMember) {
            throw new BusinessException("Access denied: not a member of this trip");
        }

        // 2. Query data with specification and pageable
        Page<Expense> expensePage = expenseRepository.findAll(
                ExpenseSpecification.filter(tripId, request),
                pageable
        );

        // 3. Map to response
        List<ExpenseResponse> content = expensePage.getContent().stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<ExpenseResponse>builder()
                .content(content)
                .pageNumber(expensePage.getNumber())
                .pageSize(expensePage.getSize())
                .totalElements(expensePage.getTotalElements())
                .totalPages(expensePage.getTotalPages())
                .last(expensePage.isLast())
                .build();
    }

    @Override
    @Transactional
    public ExpenseResponse updateExpense(Long expenseId, Long userId, UpdateExpenseRequest request) {

        if (request.getTotalAmount().compareTo(new BigDecimal("9999999999999")) > 0) {
            throw new BusinessException("Số tiền quá lớn, vui lòng kiểm tra lại");
        }

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new BusinessException("Expense not found"));

        if (Boolean.TRUE.equals(expense.getIsDeleted())) {
            throw new BusinessException("Expense has been deleted");
        }

        // Validate caller must be OWNER of the trip or payer
        Trip trip = expense.getTrip();
        validateOwnerOrPayer(trip.getId(), userId, expense.getPayer().getId());

        User newPayer = userRepository.findById(request.getPayerId())
                .orElseThrow(() -> new BusinessException("Payer not found"));

        // Validate splits sum
        validateSplitsForUpdate(request);

        // Collect all involved user IDs: splits + payer
        List<Long> userIds = request.getSplits().stream()
                .map(SplitRequest::getUserId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        if (userMap.size() != userIds.size()) {
            throw new BusinessException("Some users not found");
        }

        // Validate all users in trip
        List<Long> allInvolvedIds = new ArrayList<>(userIds);
        if (!allInvolvedIds.contains(request.getPayerId())) {
            allInvolvedIds.add(request.getPayerId());
        }
        validateUsersInTrip(trip.getId(), allInvolvedIds);

        if (request.getCategoryId() == null) {
            throw new BusinessException("Vui lòng chọn danh mục chi phí");
        }

        ExpenseCategory newCategory = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("Danh mục không tồn tại"));

        // Update basic info
        expense.setTotalAmount(request.getTotalAmount());
        expense.setDescription(request.getDescription());
        expense.setCategory(newCategory);
        expense.setExpenseDate(request.getExpenseDate());
        expense.setPayer(newPayer);

        expenseRepository.save(expense);

        // Delete old splits
        splitRepository.deleteByExpenseId(expenseId);

        // Create new splits
        List<ExpenseSplit> newSplits = request.getSplits().stream()
                .map(req -> ExpenseSplit.builder()
                        .expense(expense)
                        .user(userMap.get(req.getUserId()))
                        .amount(req.getAmount())
                        .build()
                )
                .toList();

        splitRepository.saveAll(newSplits);
        expense.setSplits(newSplits);

        return expenseMapper.toResponse(expense);
    }

    @Override
    @Transactional
    public void deleteExpense(Long expenseId, Long userId) {

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new BusinessException("Expense not found"));

        if (Boolean.TRUE.equals(expense.getIsDeleted())) {
            return;
        }

        validateOwnerOrPayer(expense.getTrip().getId(), userId, expense.getPayer().getId());

        expense.setIsDeleted(true);
        expenseRepository.save(expense);
    }

    private void validateOwnerOrPayer(Long tripId, Long callerId, Long payerId) {
        if (callerId.equals(payerId)) return;

        TripMember caller = tripMemberRepository.findByTripIdAndUserId(tripId, callerId)
                .orElseThrow(() -> new BusinessException("You are not in this trip"));
        
        if (!"OWNER".equals(caller.getRole())) {
            throw new BusinessException("Only trip OWNER or expense payer can do this");
        }
    }

    private void validateUsersInTrip(Long tripId, List<Long> userIds) {

        List<TripMember> members = tripMemberRepository.findEnabledMembersWithUser(tripId);

        Set<Long> validUserIds = members.stream()
                .map(tm -> tm.getUser().getId())
                .collect(Collectors.toSet());

        for (Long userId : userIds) {
            if (!validUserIds.contains(userId)) {
                throw new BusinessException("Thành viên (ID: " + userId + ") không hợp lệ để chia tiền (Có thể đã rời nhóm hoặc đang bị tạm ngưng).");
            }
        }
    }

    private void validateSplits(CreateExpenseRequest request) {

        List<SplitRequest> splits = request.getSplits();

        if (splits == null || splits.isEmpty()) {
            throw new BusinessException("Splits cannot be empty");
        }

        BigDecimal totalSplit = splits.stream()
                .map(SplitRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Cho phép chênh lệch (Delta) 5.0 đơn vị để bỏ qua lỗi sai số làm tròn thập phân của dấu phẩy động
        if (totalSplit.subtract(request.getTotalAmount()).abs().compareTo(new BigDecimal("5.0")) > 0) {
            throw new BusinessException("Split total must equal expense total");
        }
    }

    private void validateSplitsForUpdate(UpdateExpenseRequest request) {

        List<SplitRequest> splits = request.getSplits();

        if (splits == null || splits.isEmpty()) {
            throw new BusinessException("Splits cannot be empty");
        }

        BigDecimal totalSplit = splits.stream()
                .map(SplitRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Cho phép chênh lệch (Delta) 5.0 đơn vị để bỏ qua lỗi sai số làm tròn thập phân của dấu phẩy động
        if (totalSplit.subtract(request.getTotalAmount()).abs().compareTo(new BigDecimal("5.0")) > 0) {
            throw new BusinessException("Split total must equal expense total");
        }
    }

    @Override
    public List<com.kaitohuy.chiabill.dto.response.CategoryStatResponse> getExpenseStats(Long tripId, Long userId) {
        // 1. Verify caller is a member of the trip
        boolean isMember = tripMemberRepository.existsByTripIdAndUserId(tripId, userId);
        if (!isMember) {
            throw new BusinessException("Access denied: not a member of this trip");
        }

        // 2. Query stats from repository
        return expenseRepository.getExpenseStatsByCategory(tripId);
    }
}