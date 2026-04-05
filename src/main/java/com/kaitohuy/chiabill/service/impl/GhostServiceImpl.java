package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.request.CreateGhostMembersRequest;
import com.kaitohuy.chiabill.dto.response.GhostMemberResponse;
import com.kaitohuy.chiabill.dto.response.UserResponse;
import com.kaitohuy.chiabill.entity.*;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.mapper.UserMapper;
import com.kaitohuy.chiabill.repository.*;
import com.kaitohuy.chiabill.service.interfaces.GhostService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GhostServiceImpl implements GhostService {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final UserMapper userMapper;

    // =============================================
    // CREATE GHOST MEMBERS
    // =============================================
    @Override
    @Transactional
    public List<GhostMemberResponse> createGhostMembers(Long tripId, Long callerId, CreateGhostMembersRequest request) {

        // 1. Validate trip tồn tại
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));

        if (Boolean.TRUE.equals(trip.getIsDeleted())) {
            throw new BusinessException("Trip has been deleted");
        }

        // 2. Validate caller là OWNER
        TripMember callerMember = tripMemberRepository.findByTripIdAndUserId(tripId, callerId)
                .orElseThrow(() -> new BusinessException("You are not a member of this trip"));

        if (!"OWNER".equals(callerMember.getRole())) {
            throw new BusinessException("Only trip owner can add ghost members");
        }

        User manager = userRepository.findById(callerId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // 3. Tạo ghost users và thêm vào trip
        List<GhostMemberResponse> results = new ArrayList<>();

        for (String name : request.getNames()) {
            if (name == null || name.isBlank()) {
                throw new BusinessException("Ghost member name cannot be blank");
            }

            // Tạo Ghost User
            User ghost = User.builder()
                    .name(name.trim())
                    .isGhost(true)
                    .isAnonymous(false)
                    .allowAutoAdd(false)
                    .allowAutoApprovePayment(true)
                    .managedBy(manager)
                    .provider("GHOST")
                    .providerId("GHOST_" + UUID.randomUUID())
                    .build();

            userRepository.save(ghost);

            // Thêm vào trip_members
            TripMember member = TripMember.builder()
                    .trip(trip)
                    .user(ghost)
                    .role("MEMBER")
                    .build();

            tripMemberRepository.save(member);

            // Build response
            GhostMemberResponse response = new GhostMemberResponse();
            response.setId(ghost.getId());
            response.setName(ghost.getName());
            response.setIsGhost(true);
            response.setManagedById(callerId);
            results.add(response);
        }

        return results;
    }

    // =============================================
    // CLAIM GHOST
    // =============================================
    @Override
    @Transactional
    public UserResponse claimGhost(Long ghostId, Long realUserId) {

        // 1. Validate ghost tồn tại
        User ghost = userRepository.findByIdAndIsGhostTrue(ghostId)
                .orElseThrow(() -> new BusinessException("Ghost user not found"));

        // 2. Validate real user tồn tại
        User realUser = userRepository.findById(realUserId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (Boolean.TRUE.equals(realUser.getIsGhost())) {
            throw new BusinessException("Cannot claim ghost using another ghost account");
        }

        // 3. Validate cả real user và ghost đều phải cùng trong ít nhất 1 trip
        boolean sharedTrip = tripMemberRepository.findByUserIdAndIsActiveTrue(ghostId)
                .stream()
                .anyMatch(ghostMembership ->
                        tripMemberRepository.existsByTripIdAndUserId(
                                ghostMembership.getTrip().getId(), realUserId)
                );

        if (!sharedTrip) {
            throw new BusinessException("You must be in the same trip as the ghost member to claim it");
        }

        // 4. Chuyển tất cả ExpenseSplits từ ghost → real user
        List<ExpenseSplit> ghostSplits = expenseSplitRepository.findByUserId(ghostId);
        for (ExpenseSplit ghostSplit : ghostSplits) {
            Long expenseId = ghostSplit.getExpense().getId();

            // Kiểm tra real user đã có split trong expense này chưa (tránh duplicate unique key)
            List<ExpenseSplit> siblingRealSplits = expenseSplitRepository.findByExpenseId(expenseId)
                    .stream()
                    .filter(s -> s.getUser().getId().equals(realUserId))
                    .toList();

            if (siblingRealSplits.isEmpty()) {
                // Real user chưa có split → chuyển luôn
                ghostSplit.setUser(realUser);
                expenseSplitRepository.save(ghostSplit);
            } else {
                // Real user đã có split → merge amount rồi xóa ghost split
                ExpenseSplit realSplit = siblingRealSplits.get(0);
                realSplit.setAmount(realSplit.getAmount().add(ghostSplit.getAmount()));
                expenseSplitRepository.save(realSplit);
                expenseSplitRepository.delete(ghostSplit);
            }
        }

        // 5. Chuyển tất cả Expenses nơi ghost là payer → real user
        List<Expense> expenses = expenseRepository.findByPayerIdAndIsDeletedFalse(ghostId);
        for (Expense expense : expenses) {
            expense.setPayer(realUser);
            expenseRepository.save(expense);
        }

        // 6. Chuyển TripMember entries từ ghost → real user
        List<TripMember> ghostMemberships = tripMemberRepository.findByUserIdAndIsActiveTrue(ghostId);
        for (TripMember ghostMembership : ghostMemberships) {
            Long tripId = ghostMembership.getTrip().getId();

            Optional<TripMember> existingRealMembership =
                    tripMemberRepository.findByTripIdAndUserId(tripId, realUserId);

            if (existingRealMembership.isEmpty()) {
                // Real user chưa trong trip → chuyển membership
                ghostMembership.setUser(realUser);
                tripMemberRepository.save(ghostMembership);
            } else {
                // Real user đã trong trip → xóa ghost membership
                tripMemberRepository.delete(ghostMembership);
            }
        }

        // 7. Xóa ghost user (hard delete vì là acc ảo)
        userRepository.delete(ghost);

        return userMapper.toResponse(realUser);
    }
}
