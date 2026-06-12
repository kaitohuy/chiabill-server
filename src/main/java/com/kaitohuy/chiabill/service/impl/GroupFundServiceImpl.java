package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.request.ActivateFundRequest;
import com.kaitohuy.chiabill.dto.request.RequiredContributionRequest;
import com.kaitohuy.chiabill.dto.request.UpdateTreasurerRequest;
import com.kaitohuy.chiabill.dto.request.VoluntaryContributionRequest;
import com.kaitohuy.chiabill.dto.response.FundContributionResponse;
import com.kaitohuy.chiabill.dto.response.FundResponse;
import com.kaitohuy.chiabill.entity.*;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.mapper.GroupFundMapper;
import com.kaitohuy.chiabill.repository.*;
import com.kaitohuy.chiabill.service.interfaces.GroupFundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupFundServiceImpl implements GroupFundService {

    private final GroupFundRepository fundRepository;
    private final GroupFundContributionRepository contributionRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripMemberRepository tripMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository splitRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseCategoryRepository categoryRepository;

    private final GroupFundMapper fundMapper;

    @Override
    public FundResponse getFundByTrip(Long tripId, Long actorId) {
        validateMember(tripId, actorId);
        GroupFund fund = fundRepository.findByTripIdAndIsDeletedFalse(tripId)
                .orElseThrow(() -> new BusinessException("Quỹ chung chưa được kích hoạt cho chuyến đi này."));
        return fundMapper.toResponse(fund);
    }

    @Override
    @Transactional
    public FundResponse activateFund(Long tripId, Long actorId, ActivateFundRequest request) {
        // Bất kỳ thành viên nào của chuyến đi đều được kích hoạt quỹ
        TripMember member = tripMemberRepository.findByTripIdAndUserId(tripId, actorId)
                .orElseThrow(() -> new BusinessException("Bạn không phải thành viên của chuyến đi này."));

        if (fundRepository.findByTripIdAndIsDeletedFalse(tripId).isPresent()) {
            throw new BusinessException("Quỹ chung đã được kích hoạt trước đó.");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Chuyến đi không tồn tại."));

        Long treasurerId = request.getTreasurerId() != null ? request.getTreasurerId() : actorId;
        User treasurer = userRepository.findById(treasurerId)
                .orElseThrow(() -> new BusinessException("Người được chọn làm thủ quỹ không tồn tại."));

        // Validate thủ quỹ phải là thành viên trong trip
        if (!tripMemberRepository.existsByTripIdAndUserId(tripId, treasurerId)) {
            throw new BusinessException("Thủ quỹ phải là thành viên của chuyến đi.");
        }

        GroupFund fund = GroupFund.builder()
                .trip(trip)
                .balance(BigDecimal.ZERO)
                .currency(trip.getCurrency() != null ? trip.getCurrency() : "VND")
                .alertThreshold(request.getAlertThreshold())
                .treasurer(treasurer)
                .build();

        return fundMapper.toResponse(fundRepository.save(fund));
    }

    @Override
    @Transactional
    public FundResponse updateTreasurer(Long tripId, Long actorId, UpdateTreasurerRequest request) {
        // Chỉ OWNER mới được đổi thủ quỹ
        TripMember member = tripMemberRepository.findByTripIdAndUserId(tripId, actorId)
                .orElseThrow(() -> new BusinessException("Bạn không phải thành viên của chuyến đi này."));
        
        if (!"OWNER".equals(member.getRole())) {
            throw new BusinessException("Chỉ chủ nhóm mới được phép thay đổi thủ quỹ.");
        }

        GroupFund fund = fundRepository.findByTripIdAndIsDeletedFalse(tripId)
                .orElseThrow(() -> new BusinessException("Quỹ chung chưa được kích hoạt."));

        Long newTreasurerId = request.getTreasurerId();
        User newTreasurer = userRepository.findById(newTreasurerId)
                .orElseThrow(() -> new BusinessException("Thủ quỹ mới không tồn tại."));

        if (!tripMemberRepository.existsByTripIdAndUserId(tripId, newTreasurerId)) {
            throw new BusinessException("Thủ quỹ mới phải là thành viên của chuyến đi.");
        }

        fund.setTreasurer(newTreasurer);
        return fundMapper.toResponse(fundRepository.save(fund));
    }

    @Override
    @Transactional
    public List<FundContributionResponse> createRequiredContribution(Long tripId, Long actorId, RequiredContributionRequest request) {
        GroupFund fund = fundRepository.findByTripIdAndIsDeletedFalse(tripId)
                .orElseThrow(() -> new BusinessException("Quỹ chung chưa được kích hoạt."));

        // Chỉ Thủ quỹ hoặc OWNER được tạo đợt thu quỹ
        TripMember actorMember = tripMemberRepository.findByTripIdAndUserId(tripId, actorId)
                .orElseThrow(() -> new BusinessException("Bạn không phải thành viên của chuyến đi này."));
        
        boolean isTreasurer = fund.getTreasurer().getId().equals(actorId);
        boolean isOwner = "OWNER".equals(actorMember.getRole());
        
        if (!isTreasurer && !isOwner) {
            throw new BusinessException("Chỉ thủ quỹ hoặc chủ nhóm mới được tạo đợt thu quỹ bắt buộc.");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền thu quỹ phải lớn hơn 0.");
        }

        if (request.getContributorIds() == null || request.getContributorIds().isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất một thành viên phải đóng quỹ.");
        }

        List<User> allSelected = userRepository.findAllById(request.getContributorIds());
        if (allSelected.size() != request.getContributorIds().size()) {
            throw new BusinessException("Một số thành viên được chọn không tồn tại.");
        }

        // Validate các thành viên phải ở trong Trip
        for (Long cId : request.getContributorIds()) {
            if (!tripMemberRepository.existsByTripIdAndUserId(tripId, cId)) {
                throw new BusinessException("Thành viên (ID: " + cId + ") không thuộc chuyến đi này.");
            }
        }

        // Kiểm tra xem Thủ quỹ có tham gia đợt này không
        boolean includesTreasurer = request.getContributorIds().contains(fund.getTreasurer().getId());

        // contributors là những thành viên cần đóng quỹ NHƯNG không bao gồm Thủ quỹ (vì Thủ quỹ tự duyệt luôn)
        List<User> contributors = allSelected.stream()
                .filter(user -> !user.getId().equals(fund.getTreasurer().getId()))
                .collect(Collectors.toList());

        ExpenseCategory category = getOrCreateFundCategory(tripId, fund);

        String notes = request.getNotes() != null && !request.getNotes().trim().isEmpty() 
                ? request.getNotes().trim() : "Thu quỹ chung";

        // allParticipants là tất cả mọi người tham gia đóng đợt này (để tạo Expense Split)
        List<User> allParticipants = new ArrayList<>(allSelected);

        // Tạo Expense nộp quỹ
        BigDecimal totalAmount = request.getAmount().multiply(new BigDecimal(allParticipants.size()));
        Expense expense = createFundExpense(fund, category, notes, totalAmount);

        // Tạo splits cho tất cả participants
        List<ExpenseSplit> splits = createExpenseSplits(expense, allParticipants, request.getAmount());
        expense.setSplits(splits);

        // Tạo các bản ghi Contribution
        List<GroupFundContribution> contributions = createFundContributions(fund, contributors, request.getAmount(), notes, expense, includesTreasurer);

        // Nếu có Thủ quỹ tham gia, cộng phần của Thủ quỹ vào số dư ngay lập tức (vì auto-confirm)
        if (includesTreasurer) {
            fund.setBalance(fund.getBalance().add(request.getAmount()));
            fundRepository.save(fund);
        }

        return contributions.stream()
                .map(fundMapper::toContributionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FundContributionResponse createVoluntaryContribution(Long tripId, Long actorId, VoluntaryContributionRequest request) {
        validateMember(tripId, actorId);
        
        GroupFund fund = fundRepository.findByTripIdAndIsDeletedFalse(tripId)
                .orElseThrow(() -> new BusinessException("Quỹ chung chưa được kích hoạt."));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền đóng góp phải lớn hơn 0.");
        }

        User contributor = userRepository.findById(actorId)
                .orElseThrow(() -> new BusinessException("Thành viên không tồn tại."));

        String notes = request.getNotes() != null && !request.getNotes().trim().isEmpty()
                ? request.getNotes().trim() : "Donate quỹ chung";

        // Donate thì được duyệt luôn và cộng trực tiếp số dư quỹ
        GroupFundContribution contribution = GroupFundContribution.builder()
                .groupFund(fund)
                .contributor(contributor)
                .amount(request.getAmount())
                .contributionDate(LocalDateTime.now())
                .notes(notes)
                .type(ContributionType.VOLUNTARY)
                .isConfirmed(true) // Đóng góp tự nguyện thì auto duyệt
                .build();

        contributionRepository.save(contribution);

        // Cộng số dư quỹ
        fund.setBalance(fund.getBalance().add(request.getAmount()));
        fundRepository.save(fund);

        return fundMapper.toContributionResponse(contribution);
    }

    @Override
    public List<FundContributionResponse> getContributions(Long tripId, Long actorId) {
        validateMember(tripId, actorId);
        GroupFund fund = fundRepository.findByTripIdAndIsDeletedFalse(tripId)
                .orElseThrow(() -> new BusinessException("Quỹ chung chưa được kích hoạt."));

        List<GroupFundContribution> list = contributionRepository.findByGroupFundIdAndIsDeletedFalseOrderByContributionDateDesc(fund.getId());
        return list.stream()
                .map(fundMapper::toContributionResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FundContributionResponse confirmContribution(Long contributionId, Long actorId) {
        GroupFundContribution contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new BusinessException("Bản ghi đóng quỹ không tồn tại."));

        if (Boolean.TRUE.equals(contribution.getIsDeleted())) {
            throw new BusinessException("Bản ghi đóng quỹ đã bị xoá.");
        }

        GroupFund fund = contribution.getGroupFund();
        
        // Chỉ Thủ quỹ hoặc OWNER được xác nhận đóng quỹ
        TripMember actorMember = tripMemberRepository.findByTripIdAndUserId(fund.getTrip().getId(), actorId)
                .orElseThrow(() -> new BusinessException("Bạn không phải thành viên của chuyến đi này."));

        boolean isTreasurer = fund.getTreasurer().getId().equals(actorId);
        boolean isOwner = "OWNER".equals(actorMember.getRole());

        if (!isTreasurer && !isOwner) {
            throw new BusinessException("Chỉ thủ quỹ hoặc chủ nhóm mới được xác nhận đã đóng quỹ.");
        }

        if (Boolean.TRUE.equals(contribution.getIsConfirmed())) {
            throw new BusinessException("Bản ghi này đã được xác nhận trước đó.");
        }

        // Cập nhật trạng thái xác nhận
        contribution.setIsConfirmed(true);
        contributionRepository.save(contribution);

        // Cộng số dư quỹ chung
        fund.setBalance(fund.getBalance().add(contribution.getAmount()));
        fundRepository.save(fund);

        // Tạo một Payment APPROVED từ contributor đến Thủ quỹ để xoá công nợ trong tab Quyết toán
        Payment payment = Payment.builder()
                .trip(fund.getTrip())
                .fromUser(contribution.getContributor()) // Người nộp
                .toUser(fund.getTreasurer()) // Người nhận (thủ quỹ)
                .amount(contribution.getAmount())
                .status(PaymentStatus.APPROVED)
                .proofUrl(null)
                .onBehalfOfUser(null)
                .linkedContribution(contribution) // Link để có thể reverse khi cần
                .build();
        
        paymentRepository.save(payment);

        return fundMapper.toContributionResponse(contribution);
    }

    private ExpenseCategory getOrCreateFundCategory(Long tripId, GroupFund fund) {
        return categoryRepository.findAllByTripIdOrSystem(tripId).stream()
                .filter(c -> c.getName().toLowerCase().contains("quỹ"))
                .findFirst()
                .orElseGet(() -> {
                    ExpenseCategory newCat = ExpenseCategory.builder()
                            .name("Quỹ chung")
                            .icon("💰")
                            .trip(fund.getTrip())
                            .build();
                    return categoryRepository.save(newCat);
                });
    }

    private Expense createFundExpense(GroupFund fund, ExpenseCategory category, String notes, BigDecimal totalAmount) {
        Expense expense = Expense.builder()
                .trip(fund.getTrip())
                .payer(fund.getTreasurer())
                .totalAmount(totalAmount)
                .description("Yêu cầu đóng quỹ: " + notes)
                .category(category)
                .expenseDate(LocalDateTime.now())
                .currency(fund.getCurrency())
                .exchangeRate(BigDecimal.ONE)
                .clientUuid(UUID.randomUUID().toString())
                .isFromFund(false)     // Phải là false để tính vào công nợ nợ nần (mọi người nợ thủ quỹ)
                .groupFund(fund)       // Link tới quỹ chung để tiện query/reverse
                .build();
        return expenseRepository.save(expense);
    }

    private List<ExpenseSplit> createExpenseSplits(Expense expense, List<User> allParticipants, BigDecimal amount) {
        List<ExpenseSplit> splits = allParticipants.stream()
                .map(user -> ExpenseSplit.builder()
                        .expense(expense)
                        .user(user)
                        .amount(amount)
                        .percentage(BigDecimal.ZERO)
                        .isSettled(false)
                        .build()
                )
                .collect(Collectors.toList());
        return splitRepository.saveAll(splits);
    }

    private List<GroupFundContribution> createFundContributions(GroupFund fund, List<User> contributors, BigDecimal amount, String notes, Expense linkedExpense, boolean includesTreasurer) {
        List<GroupFundContribution> contributions = contributors.stream()
                .map(user -> GroupFundContribution.builder()
                        .groupFund(fund)
                        .contributor(user)
                        .amount(amount)
                        .contributionDate(LocalDateTime.now())
                        .notes(notes)
                        .type(ContributionType.REQUIRED)
                        .isConfirmed(false)
                        .linkedExpense(linkedExpense)
                        .build()
                )
                .collect(Collectors.toList());

        // Chỉ tự động đóng góp phần của Thủ quỹ nếu Thủ quỹ được chọn tham gia
        if (includesTreasurer) {
            GroupFundContribution treasurerContribution = GroupFundContribution.builder()
                    .groupFund(fund)
                    .contributor(fund.getTreasurer())
                    .amount(amount)
                    .contributionDate(LocalDateTime.now())
                    .notes(notes + " (Thủ quỹ tự nộp)")
                    .type(ContributionType.REQUIRED)
                    .isConfirmed(true)
                    .linkedExpense(linkedExpense)
                    .build();
            
            contributions.add(treasurerContribution);
        }
        return contributionRepository.saveAll(contributions);
    }

    private void validateMember(Long tripId, Long userId) {
        boolean exists = tripMemberRepository.existsByTripIdAndUserId(tripId, userId);
        if (!exists) {
            throw new BusinessException("Bạn không phải thành viên của chuyến đi này.");
        }
    }
}
