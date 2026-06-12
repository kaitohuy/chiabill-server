package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.request.CreateExpenseRequest;
import com.kaitohuy.chiabill.dto.request.SearchExpenseRequest;
import com.kaitohuy.chiabill.dto.request.SplitRequest;
import com.kaitohuy.chiabill.dto.request.UpdateExpenseRequest;
import com.kaitohuy.chiabill.dto.response.ExpenseResponse;
import com.kaitohuy.chiabill.dto.response.PageResponse;
import com.kaitohuy.chiabill.dto.response.ScanReceiptResponse;
import com.kaitohuy.chiabill.entity.*;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.mapper.ExpenseMapper;
import com.kaitohuy.chiabill.repository.*;
import com.kaitohuy.chiabill.repository.specification.ExpenseSpecification;
import com.kaitohuy.chiabill.service.interfaces.CloudinaryService;
import com.kaitohuy.chiabill.service.interfaces.GeminiService;
import com.kaitohuy.chiabill.service.interfaces.NotificationService;
import com.kaitohuy.chiabill.service.interfaces.TripHistoryService;
import com.kaitohuy.chiabill.utils.CurrencyUtil;

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
    private final CloudinaryService cloudinaryService;
    private final TripHistoryService tripHistoryService;

    private final GroupFundRepository fundRepository;
    private final GroupFundContributionRepository contributionRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseMapper expenseMapper;
    private final GeminiService geminiService;

    @Override
    @Transactional
    public ExpenseResponse createExpense(Long actorId, CreateExpenseRequest request) {
        if (request.getTotalAmount().compareTo(new java.math.BigDecimal("9999999999999")) > 0) {
            throw new BusinessException("Số tiền quá lớn, vui lòng kiểm tra lại");
        }

        // Chống trùng lặp bằng clientUuid
        if (request.getClientUuid() != null && !request.getClientUuid().trim().isEmpty()) {
            java.util.Optional<Expense> existing = expenseRepository.findByClientUuid(request.getClientUuid());
            if (existing.isPresent()) {
                return expenseMapper.toResponse(existing.get());
            }
        }

        // Validate trip exists
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new BusinessException("Chuyến đi không tồn tại"));

        // Validate payer exists
        User payer = userRepository.findById(request.getPayerId())
                .orElseThrow(() -> new BusinessException("Người chi không tồn tại"));
                
        // Fetch actor
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new BusinessException("Actor not found"));

        // Validate actor in trip and not suspended/disabled
        TripMember actorMember = tripMemberRepository.findByTripIdAndUserId(trip.getId(), actorId)
                .orElseThrow(() -> new BusinessException("Bạn không phải thành viên của chuyến đi này"));
        if (!Boolean.TRUE.equals(actorMember.getIsActive())) {
            throw new BusinessException("Bạn đã rời khỏi chuyến đi này.");
        }
        if (actorMember.getStatus() == com.kaitohuy.chiabill.entity.MemberStatus.DISABLED) {
            throw new BusinessException("Tài khoản của bạn đang bị tạm ngưng hoạt động trong chuyến đi này.");
        }

        if (Boolean.TRUE.equals(trip.getIsDeleted())) {
            throw new BusinessException("Trip has been deleted");
        }

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

        if (request.getExpenseDate() == null) {
            request.setExpenseDate(java.time.LocalDateTime.now());
        }

        // Logic Quỹ chung
        GroupFund targetFund = null;
        boolean isFromFund = Boolean.TRUE.equals(request.getIsFromFund());
        if (isFromFund) {
            targetFund = fundRepository.findByTripIdAndIsDeletedFalse(trip.getId())
                    .orElseThrow(() -> new BusinessException("Quỹ chung chưa được kích hoạt cho chuyến đi này."));
            if (targetFund.getBalance().compareTo(request.getTotalAmount()) < 0) {
                throw new BusinessException("Số dư quỹ chung không đủ để thực hiện thanh toán này (Số dư quỹ hiện tại: " + targetFund.getBalance() + ").");
            }
            targetFund.setBalance(targetFund.getBalance().subtract(request.getTotalAmount()));
            fundRepository.save(targetFund);
        }

        Expense expense = Expense.builder()
                .trip(trip)
                .payer(payer)
                .totalAmount(request.getTotalAmount())
                .description(request.getDescription())
                .category(category)
                .expenseDate(request.getExpenseDate())
                .receiptUrl(request.getReceiptUrl())
                .currency(request.getCurrency() != null ? request.getCurrency() : trip.getCurrency())
                .exchangeRate(request.getExchangeRate() != null ? request.getExchangeRate() : BigDecimal.ONE)
                .isFromFund(isFromFund)
                .groupFund(targetFund)
                .clientUuid(request.getClientUuid())
                .splitType(request.getSplitType() != null ? request.getSplitType() : "EQUAL")
                .build();

        expenseRepository.save(expense);

        List<ExpenseSplit> splits = request.getSplits().stream()
                .map(req -> ExpenseSplit.builder()
                        .expense(expense)
                        .user(userMap.get(req.getUserId()))
                        .amount(req.getAmount())
                        .splitValue(req.getSplitValue())
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
                            payer.getName() + " vừa thêm " + CurrencyUtil.format(expense.getTotalAmount()) + " cho " + category.getName(),

                            NotificationType.EXPENSE_CREATED,
                            trip.getId()
                    );
                }
            }
        } catch (Exception e) {
            // Không để lỗi gửi thông báo làm hỏng transaction chính của Expense
        }

        // 🚀 Log activity
        tripHistoryService.logAddExpense(actor, expense);

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

        if (expensePage.isEmpty()) {
            return PageResponse.<ExpenseResponse>builder()
                    .content(List.of())
                    .pageNumber(expensePage.getNumber())
                    .pageSize(expensePage.getSize())
                    .totalElements(expensePage.getTotalElements())
                    .totalPages(expensePage.getTotalPages())
                    .last(expensePage.isLast())
                    .build();
        }

        // 3. Batch load full details for the page (Payer, Category, Splits)
        List<Long> ids = expensePage.getContent().stream()
                .map(Expense::getId)
                .toList();
        
        List<Expense> detailedExpenses = expenseRepository.findAllByIdInWithPayerAndCategoryAndSplits(ids);
        
        Map<Long, Expense> detailedMap = detailedExpenses.stream()
                .collect(Collectors.toMap(Expense::getId, e -> e));

        // 4. Map to response preserving original order from expensePage
        List<ExpenseResponse> content = expensePage.getContent().stream()
                .map(e -> detailedMap.get(e.getId()))
                .filter(Objects::nonNull) // Safety check
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

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Actor not found"));

        // Capture OLD state for logging
        Expense oldState = Expense.builder()
                .totalAmount(expense.getTotalAmount())
                .category(expense.getCategory())
                .description(expense.getDescription())
                .payer(expense.getPayer())
                .trip(expense.getTrip())
                .build();

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
        // Handle receipt image deletion if updated
        if (request.getReceiptUrl() != null) {
            String newReceipt = request.getReceiptUrl().trim();
            if (newReceipt.isEmpty()) {
                if (expense.getReceiptUrl() != null) {
                    cloudinaryService.deleteImage(expense.getReceiptUrl());
                }
                expense.setReceiptUrl(null);
            } else if (!newReceipt.equals(expense.getReceiptUrl())) {
                if (expense.getReceiptUrl() != null) {
                    cloudinaryService.deleteImage(expense.getReceiptUrl());
                }
                expense.setReceiptUrl(newReceipt);
            }
        }

        // Logic Quỹ chung khi cập nhật Expense
        boolean oldIsFromFund = Boolean.TRUE.equals(expense.getIsFromFund());
        boolean newIsFromFund = Boolean.TRUE.equals(request.getIsFromFund());
        BigDecimal oldAmount = expense.getTotalAmount();
        BigDecimal newAmount = request.getTotalAmount();

        if (oldIsFromFund && !newIsFromFund) {
            GroupFund fund = expense.getGroupFund();
            if (fund == null) {
                fund = fundRepository.findByTripIdAndIsDeletedFalse(trip.getId())
                        .orElseThrow(() -> new BusinessException("Quỹ chung không tồn tại"));
            }
            fund.setBalance(fund.getBalance().add(oldAmount));
            fundRepository.save(fund);
            expense.setIsFromFund(false);
            expense.setGroupFund(null);
        } else if (!oldIsFromFund && newIsFromFund) {
            GroupFund fund = fundRepository.findByTripIdAndIsDeletedFalse(trip.getId())
                    .orElseThrow(() -> new BusinessException("Quỹ chung chưa được kích hoạt cho chuyến đi này."));
            if (fund.getBalance().compareTo(newAmount) < 0) {
                throw new BusinessException("Số dư quỹ chung không đủ để thực hiện thanh toán này (Số dư quỹ hiện tại: " + fund.getBalance() + ").");
            }
            fund.setBalance(fund.getBalance().subtract(newAmount));
            fundRepository.save(fund);
            expense.setIsFromFund(true);
            expense.setGroupFund(fund);
        } else if (oldIsFromFund && newIsFromFund) {
            GroupFund fund = expense.getGroupFund();
            if (fund == null) {
                fund = fundRepository.findByTripIdAndIsDeletedFalse(trip.getId())
                        .orElseThrow(() -> new BusinessException("Quỹ chung không tồn tại"));
            }
            BigDecimal diff = newAmount.subtract(oldAmount);
            if (diff.compareTo(BigDecimal.ZERO) > 0 && fund.getBalance().compareTo(diff) < 0) {
                throw new BusinessException("Số dư quỹ chung không đủ để cập nhật thanh toán này (Số dư quỹ hiện tại: " + fund.getBalance() + ", cần thêm: " + diff + ").");
            }
            fund.setBalance(fund.getBalance().subtract(diff));
            fundRepository.save(fund);
            expense.setIsFromFund(true);
            expense.setGroupFund(fund);
        }

        expense.setTotalAmount(request.getTotalAmount());
        expense.setDescription(request.getDescription());
        expense.setCategory(newCategory);
        expense.setExpenseDate(request.getExpenseDate());
        expense.setPayer(newPayer);
        if (request.getCurrency() != null) {
            expense.setCurrency(request.getCurrency());
        }
        if (request.getExchangeRate() != null) {
            expense.setExchangeRate(request.getExchangeRate());
        }
        if (request.getSplitType() != null) {
            expense.setSplitType(request.getSplitType());
        }

        expenseRepository.save(expense);

        // Delete old splits
        splitRepository.deleteByExpenseId(expenseId);

        // Create new splits
        List<ExpenseSplit> newSplits = request.getSplits().stream()
                .map(req -> ExpenseSplit.builder()
                        .expense(expense)
                        .user(userMap.get(req.getUserId()))
                        .amount(req.getAmount())
                        .splitValue(req.getSplitValue())
                        .build()
                )
                .toList();

        splitRepository.saveAll(newSplits);
        expense.setSplits(newSplits);

        // 🚀 Log activity
        tripHistoryService.logEditExpense(actor, oldState, expense);

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

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Actor not found"));

        // === CASCADE: Nếu expense này là đợt thu quỹ bắt buộc, reverse tất cả contributions + payments liên quan ===
        List<GroupFundContribution> linkedContributions = contributionRepository.findByLinkedExpenseIdAndIsDeletedFalse(expenseId);
        if (!linkedContributions.isEmpty()) {
            GroupFund fund = linkedContributions.get(0).getGroupFund();
            for (GroupFundContribution contribution : linkedContributions) {
                // Nếu contribution đã được xác nhận, cần trừ lại số dư quỹ và xóa payment
                if (Boolean.TRUE.equals(contribution.getIsConfirmed())) {
                    // Hoàn lại số dư quỹ
                    fund.setBalance(fund.getBalance().subtract(contribution.getAmount()));
                    // Xóa payment liên quan (nếu có)
                    paymentRepository.findByLinkedContributionIdAndIsDeletedFalse(contribution.getId())
                            .ifPresent(payment -> {
                                payment.setIsDeleted(true);
                                paymentRepository.save(payment);
                            });
                }
                // Soft-delete contribution
                contribution.setIsDeleted(true);
                contributionRepository.save(contribution);
            }
            // Lưu lại số dư quỹ sau khi reverse
            fundRepository.save(fund);
        }

        expense.setIsDeleted(true);
        expenseRepository.save(expense);

        // 🚀 Log activity
        tripHistoryService.logDeleteExpense(actor, expense);
    }

    private void validateOwnerOrPayer(Long tripId, Long callerId, Long payerId) {
        TripMember caller = tripMemberRepository.findByTripIdAndUserId(tripId, callerId)
                .orElseThrow(() -> new BusinessException("Bạn không phải thành viên của chuyến đi này"));
        
        if (!Boolean.TRUE.equals(caller.getIsActive())) {
            throw new BusinessException("Bạn đã rời khỏi chuyến đi này.");
        }
        if (caller.getStatus() == com.kaitohuy.chiabill.entity.MemberStatus.DISABLED) {
            throw new BusinessException("Tài khoản của bạn đang bị tạm ngưng hoạt động trong chuyến đi này.");
        }

        if (callerId.equals(payerId)) return;
        
        if (!"OWNER".equals(caller.getRole())) {
            throw new BusinessException("Chỉ chủ nhóm hoặc người tạo chi phí mới được phép thao tác");
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

    @Override
    public List<com.kaitohuy.chiabill.dto.response.TripStatResponse> getOverallExpenseStats(Long userId, Integer month, Integer year) {
        return expenseRepository.getOverallExpenseStats(userId, month, year);
    }

    @Override
    public java.math.BigDecimal getLatestExchangeRate(String currency) {
        return expenseRepository.findLatestExchangeRateByCurrency(currency);
    }

    @Override
    public ScanReceiptResponse scanReceipt(Long tripId, Long userId, org.springframework.web.multipart.MultipartFile file) {
        // 1. Verify member
        boolean isMember = tripMemberRepository.existsByTripIdAndUserId(tripId, userId);
        if (!isMember) {
            throw new BusinessException("Bạn không phải thành viên của chuyến đi này");
        }

        // 2. Fetch categories for trip
        List<ExpenseCategory> categories = categoryRepository.findAllByTripIdOrSystem(tripId)
                .stream()
                .filter(ec -> !Boolean.TRUE.equals(ec.getIsDeleted()))
                .toList();

        List<String> categoryNames = categories.stream()
                .map(ExpenseCategory::getName)
                .toList();

        try {
            // 3. Call Gemini service
            byte[] imageBytes = file.getBytes();
            String mimeType = file.getContentType();
            
            Map<String, Object> geminiResult = geminiService.scanReceipt(imageBytes, mimeType, categoryNames);

            // Kiểm tra ảnh có phải hóa đơn hợp lệ không
            Object isReceiptObj = geminiResult.get("isReceipt");
            if (isReceiptObj != null) {
                boolean isReceipt = Boolean.parseBoolean(isReceiptObj.toString());
                if (!isReceipt) {
                    throw new BusinessException("Hình ảnh tải lên không chứa thông tin hóa đơn hoặc biên lai chi tiêu. Vui lòng chọn lại ảnh hoặc nhập tay thông tin.");
                }
            }

            // 4. Map values
            BigDecimal totalAmount = BigDecimal.ZERO;
            Object amountObj = geminiResult.get("totalAmount");
            if (amountObj != null) {
                try {
                    totalAmount = new BigDecimal(amountObj.toString());
                } catch (Exception e) {
                    // Fallback parse if Gemini returns string with formatting
                    String cleanAmount = amountObj.toString().replaceAll("[^\\d.]", "");
                    if (!cleanAmount.isEmpty()) {
                        totalAmount = new java.math.BigDecimal(cleanAmount);
                    }
                }
            }

            String description = (String) geminiResult.get("description");
            if (description == null) {
                description = "";
            }

            String matchedCategoryName = (String) geminiResult.get("categoryName");
            ExpenseCategory matchedCategory = null;
            if (matchedCategoryName != null) {
                String cleanMatchedName = matchedCategoryName.trim();
                matchedCategory = categories.stream()
                        .filter(ec -> ec.getName().equalsIgnoreCase(cleanMatchedName))
                        .findFirst()
                        .orElse(null);
                
                if (matchedCategory == null) {
                    matchedCategory = categories.stream()
                            .filter(ec -> ec.getName().toLowerCase().contains(cleanMatchedName.toLowerCase()))
                            .findFirst()
                            .orElse(null);
                }
            }

            // Fallback to miscellaneous/other if no category matched
            if (matchedCategory == null) {
                matchedCategory = categories.stream()
                        .filter(ec -> ec.getName().toLowerCase().contains("phát sinh") || ec.getName().toLowerCase().contains("khác"))
                        .findFirst()
                        .orElse(null);
            }

            if (matchedCategory == null && !categories.isEmpty()) {
                matchedCategory = categories.get(0);
            }

            return com.kaitohuy.chiabill.dto.response.ScanReceiptResponse.builder()
                    .totalAmount(totalAmount)
                    .description(description)
                    .categoryId(matchedCategory != null ? matchedCategory.getId() : null)
                    .categoryName(matchedCategory != null ? matchedCategory.getName() : null)
                    .categoryIcon(matchedCategory != null ? matchedCategory.getIcon() : null)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Lỗi đọc dữ liệu hóa đơn: " + e.getMessage());
        }
    }
}