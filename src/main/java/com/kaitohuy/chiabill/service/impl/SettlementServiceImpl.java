package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.response.SettlementResponse;
import com.kaitohuy.chiabill.dto.response.SettlementSummaryResponse;
import com.kaitohuy.chiabill.entity.Expense;
import com.kaitohuy.chiabill.entity.ExpenseSplit;
import com.kaitohuy.chiabill.entity.Payment;
import com.kaitohuy.chiabill.entity.PaymentStatus;
import com.kaitohuy.chiabill.entity.TripMember;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.repository.ExpenseRepository;
import com.kaitohuy.chiabill.repository.PaymentRepository;
import com.kaitohuy.chiabill.repository.TripMemberRepository;
import com.kaitohuy.chiabill.service.interfaces.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;
    private final TripMemberRepository tripMemberRepository;

    @Override
    public List<SettlementResponse> calculateSettlement(Long tripId, Long userId) {

        // 1. Kiểm tra member
        boolean isMember = tripMemberRepository.existsByTripIdAndUserId(tripId, userId);
        if (!isMember) {
            throw new BusinessException("Access denied: not a member of this trip");
        }

        // 2. Lấy toàn bộ dữ liệu & Tính Net Balance
        List<TripMember> members = tripMemberRepository.findByTripId(tripId);
        Map<Long, BigDecimal> netBalances = calculateNetBalances(tripId, members);

        // Map để lưu trữ thông tin cơ bản
        Map<Long, String> nameMap = new HashMap<>();
        Map<Long, Boolean> activeMap = new HashMap<>();

        for (TripMember member : members) {
            Long mid = member.getUser().getId();
            nameMap.put(mid, member.getUser().getName());
            activeMap.put(mid, member.getIsActive());
        }


        // 4. Phân loại Chủ nợ (Creditors) và Con nợ (Debtors)
        List<UserBalance> creditors = new ArrayList<>();
        List<UserBalance> debtors = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> entry : netBalances.entrySet()) {
            BigDecimal bal = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            if (bal.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new UserBalance(entry.getKey(), bal));
            } else if (bal.compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new UserBalance(entry.getKey(), bal.abs()));
            }
        }

        // 5. Thuật toán Greedy để khớp nợ (Minimize Cash Flow)
        List<SettlementResponse> result = new ArrayList<>();
        
        int cIdx = 0;
        int dIdx = 0;

        while (cIdx < creditors.size() && dIdx < debtors.size()) {
            UserBalance creditor = creditors.get(cIdx);
            UserBalance debtor = debtors.get(dIdx);

            BigDecimal settleAmount = creditor.balance.min(debtor.balance);

            if (settleAmount.compareTo(new BigDecimal("0.01")) > 0) {
                SettlementResponse res = new SettlementResponse();
                res.setFromUserId(debtor.userId);
                res.setFromUserName(nameMap.get(debtor.userId));
                res.setToUserId(creditor.userId);
                res.setToUserName(nameMap.get(creditor.userId));
                res.setAmount(settleAmount);
                res.setFromUserActive(activeMap.getOrDefault(debtor.userId, false));
                res.setToUserActive(activeMap.getOrDefault(creditor.userId, false));
                result.add(res);
            }

            creditor.balance = creditor.balance.subtract(settleAmount);
            debtor.balance = debtor.balance.subtract(settleAmount);

            if (creditor.balance.compareTo(new BigDecimal("0.01")) < 0) cIdx++;
            if (debtor.balance.compareTo(new BigDecimal("0.01")) < 0) dIdx++;
        }

        return result;
    }

    @Override
    public SettlementSummaryResponse getSettlementSummary(Long userId) {
        List<TripMember> memberships = tripMemberRepository.findByUserIdAndIsActiveTrue(userId);

        BigDecimal totalOwed = BigDecimal.ZERO;
        BigDecimal totalReceivable = BigDecimal.ZERO;

        for (TripMember member : memberships) {
            Long tripId = member.getTrip().getId();
            List<TripMember> allTripMembers = tripMemberRepository.findByTripId(tripId);
            Map<Long, BigDecimal> balances = calculateNetBalances(tripId, allTripMembers);

            BigDecimal userBalance = balances.getOrDefault(userId, BigDecimal.ZERO);

            if (userBalance.compareTo(BigDecimal.ZERO) > 0) {
                totalReceivable = totalReceivable.add(userBalance);
            } else if (userBalance.compareTo(BigDecimal.ZERO) < 0) {
                totalOwed = totalOwed.add(userBalance.abs());
            }
        }

        return SettlementSummaryResponse.builder()
                .totalOwed(totalOwed.setScale(2, RoundingMode.HALF_UP))
                .totalReceivable(totalReceivable.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private Map<Long, BigDecimal> calculateNetBalances(Long tripId, List<TripMember> members) {
        List<Expense> expenses = expenseRepository.fetchAllDataForSettlement(tripId);
        List<Payment> payments = paymentRepository.findByTripIdAndStatus(tripId, PaymentStatus.APPROVED);

        Map<Long, BigDecimal> netBalances = new HashMap<>();
        for (TripMember member : members) {
            netBalances.put(member.getUser().getId(), BigDecimal.ZERO);
        }

        // Processing Expenses
        for (Expense expense : expenses) {
            Long payerId = expense.getPayer().getId();
            for (ExpenseSplit split : expense.getSplits()) {
                Long debtorId = split.getUser().getId();
                BigDecimal amount = split.getAmount();

                if (!debtorId.equals(payerId)) {
                    // Payer gets credit
                    netBalances.put(payerId, netBalances.getOrDefault(payerId, BigDecimal.ZERO).add(amount));
                    // Debtor gets debit
                    netBalances.put(debtorId, netBalances.getOrDefault(debtorId, BigDecimal.ZERO).subtract(amount));
                }
            }
        }

        // Processing Payments
        for (Payment payment : payments) {
            Long fromId = payment.getFromUser().getId();
            Long toId = payment.getToUser().getId();
            BigDecimal amount = payment.getAmount();

            netBalances.put(fromId, netBalances.getOrDefault(fromId, BigDecimal.ZERO).add(amount));
            netBalances.put(toId, netBalances.getOrDefault(toId, BigDecimal.ZERO).subtract(amount));
        }

        return netBalances;
    }

    private static class UserBalance {
        Long userId;
        BigDecimal balance;

        UserBalance(Long userId, BigDecimal balance) {
            this.userId = userId;
            this.balance = balance;
        }
    }
}