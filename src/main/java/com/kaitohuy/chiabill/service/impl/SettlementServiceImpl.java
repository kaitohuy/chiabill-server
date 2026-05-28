package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.response.ExpenseResponse;
import com.kaitohuy.chiabill.dto.response.PersonalStatementResponse;
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
import com.kaitohuy.chiabill.repository.UserRepository;
import com.kaitohuy.chiabill.service.interfaces.SettlementService;
import com.kaitohuy.chiabill.mapper.ExpenseMapper;
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
    private final UserRepository userRepository;
    private final ExpenseMapper expenseMapper;

    @Override
    public List<SettlementResponse> calculateSettlement(Long tripId, Long userId) {

        // 1. Kiểm tra member
        boolean isMember = tripMemberRepository.existsByTripIdAndUserId(tripId, userId);
        if (!isMember) {
            throw new BusinessException("Access denied: not a member of this trip");
        }

        // 2. Lấy toàn bộ dữ liệu & Tính Net Balance
        List<TripMember> members = tripMemberRepository.findByTripId(tripId);
        List<Expense> expenses = expenseRepository.fetchAllDataForSettlement(tripId);
        List<Payment> payments = paymentRepository.findByTripIdAndStatus(tripId, PaymentStatus.APPROVED);
        Map<Long, BigDecimal> netBalances = calculateNetBalances(members, expenses, payments);

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
    public PersonalStatementResponse getPersonalStatement(Long tripId, Long actorId, Long targetUserId) {
        // 1. Kiểm tra actor có trong trip không
        boolean isMember = tripMemberRepository.existsByTripIdAndUserId(tripId, actorId);
        if (!isMember) {
            throw new BusinessException("Access denied: not a member of this trip");
        }

        // 2. Fetch data
        List<Expense> expenses = expenseRepository.fetchAllDataForSettlement(tripId);
        List<Payment> payments = paymentRepository.findByTripIdAndStatus(tripId, PaymentStatus.APPROVED);

        BigDecimal totalSpent = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        List<ExpenseResponse> involvedExpenses = new ArrayList<>();

        for (Expense expense : expenses) {
            if (Boolean.TRUE.equals(expense.getIsFromFund())) {
                continue; // Bỏ qua chi tiêu từ Quỹ chung
            }

            boolean isInvolved = false;
            
            // Tính số tiền đã trả cho expense này
            if (expense.getPayer().getId().equals(targetUserId)) {
                totalPaid = totalPaid.add(expense.getTotalAmount());
                isInvolved = true;
            }

            // Tính số tiền đã tiêu (phần mình chịu) trong expense này
            for (ExpenseSplit split : expense.getSplits()) {
                if (split.getUser().getId().equals(targetUserId)) {
                    totalSpent = totalSpent.add(split.getAmount());
                    isInvolved = true;
                }
            }

            if (isInvolved) {
                involvedExpenses.add(expenseMapper.toResponse(expense));
            }
        }

        // 3. Tính cả phần Payment (thanh toán nợ)
        for (Payment payment : payments) {
            if (payment.getFromUser().getId().equals(targetUserId)) {
                totalPaid = totalPaid.add(payment.getAmount());
            } else if (payment.getToUser().getId().equals(targetUserId)) {
                totalSpent = totalSpent.add(payment.getAmount());
            }
        }

        BigDecimal netBalance = totalPaid.subtract(totalSpent);
        String targetUserName = userRepository.findById(targetUserId).map(com.kaitohuy.chiabill.entity.User::getName).orElse("Unknown");

        return PersonalStatementResponse.builder()
                .userId(targetUserId)
                .userName(targetUserName)
                .totalPaid(totalPaid.setScale(2, RoundingMode.HALF_UP))
                .totalSpent(totalSpent.setScale(2, RoundingMode.HALF_UP))
                .netBalance(netBalance.setScale(2, RoundingMode.HALF_UP))
                .involvedExpenses(involvedExpenses)
                .build();
    }

    @Override
    public SettlementSummaryResponse getSettlementSummary(Long userId) {
        List<TripMember> memberships = tripMemberRepository.findByUserIdAndIsActiveTrue(userId);

        BigDecimal totalOwed = BigDecimal.ZERO;
        BigDecimal totalReceivable = BigDecimal.ZERO;

        if (memberships.isEmpty()) {
            return SettlementSummaryResponse.builder()
                    .totalOwed(totalOwed)
                    .totalReceivable(totalReceivable)
                    .build();
        }

        List<Long> tripIds = memberships.stream()
                .map(m -> m.getTrip().getId())
                .collect(Collectors.toList());

        // Batch Fetch Data
        List<TripMember> allTripMembers = tripMemberRepository.findByTripIdIn(tripIds);
        List<Expense> allExpenses = expenseRepository.fetchAllDataForSettlementIn(tripIds);
        List<Payment> allPayments = paymentRepository.findByTripIdInAndStatus(tripIds, PaymentStatus.APPROVED);

        // Group Data By tripId in-memory
        Map<Long, List<TripMember>> membersByTrip = allTripMembers.stream()
                .collect(Collectors.groupingBy(m -> m.getTrip().getId()));
        Map<Long, List<Expense>> expensesByTrip = allExpenses.stream()
                .collect(Collectors.groupingBy(e -> e.getTrip().getId()));
        Map<Long, List<Payment>> paymentsByTrip = allPayments.stream()
                .collect(Collectors.groupingBy(p -> p.getTrip().getId()));

        for (Long tripId : tripIds) {
            List<TripMember> members = membersByTrip.getOrDefault(tripId, Collections.emptyList());
            List<Expense> expenses = expensesByTrip.getOrDefault(tripId, Collections.emptyList());
            List<Payment> payments = paymentsByTrip.getOrDefault(tripId, Collections.emptyList());

            Map<Long, BigDecimal> balances = calculateNetBalances(members, expenses, payments);

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

    private Map<Long, BigDecimal> calculateNetBalances(List<TripMember> members, List<Expense> expenses, List<Payment> payments) {

        Map<Long, BigDecimal> netBalances = new HashMap<>();
        for (TripMember member : members) {
            netBalances.put(member.getUser().getId(), BigDecimal.ZERO);
        }

        // Processing Expenses
        for (Expense expense : expenses) {
            if (Boolean.TRUE.equals(expense.getIsFromFund())) {
                continue; // Bỏ qua chi tiêu từ Quỹ chung
            }
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