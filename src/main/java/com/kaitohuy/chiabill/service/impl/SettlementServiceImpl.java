package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.response.SettlementResponse;
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

        // 2. Lấy toàn bộ dữ liệu
        List<Expense> expenses = expenseRepository.fetchAllDataForSettlement(tripId);
        List<Payment> payments = paymentRepository.findByTripIdAndStatus(tripId, PaymentStatus.APPROVED);
        List<TripMember> members = tripMemberRepository.findByTripId(tripId);

        // Map để lưu trữ thông tin cơ bản
        Map<Long, String> nameMap = new HashMap<>();
        Map<Long, Boolean> activeMap = new HashMap<>();
        Map<Long, BigDecimal> netBalances = new HashMap<>();

        for (TripMember member : members) {
            Long mid = member.getUser().getId();
            nameMap.put(mid, member.getUser().getName());
            activeMap.put(mid, member.getIsActive());
            netBalances.put(mid, BigDecimal.ZERO);
        }

        // 3. Tính toán Net Balance cho từng người
        // Balance = (Số tiền người khác nợ mình) - (Số tiền mình nợ người khác)
        
        // Xử lý Expenses
        for (Expense expense : expenses) {
            Long payerId = expense.getPayer().getId();
            for (ExpenseSplit split : expense.getSplits()) {
                Long debtorId = split.getUser().getId();
                BigDecimal amount = split.getAmount();

                if (!debtorId.equals(payerId)) {
                    // Payer được cộng tiền (người khác nợ họ)
                    netBalances.put(payerId, netBalances.get(payerId).add(amount));
                    // Debtor bị trừ tiền (họ nợ người khác)
                    netBalances.put(debtorId, netBalances.get(debtorId).subtract(amount));
                }
            }
        }

        // Xử lý Approved Payments (Người dùng trả nợ trực tiếp cho nhau)
        for (Payment payment : payments) {
            Long fromId = payment.getFromUser().getId(); // Người trả
            Long toId = payment.getToUser().getId();     // Người nhận
            BigDecimal amount = payment.getAmount();

            // FromUser trả nợ -> Giảm số tiền họ nợ (Tăng balance)
            netBalances.put(fromId, netBalances.get(fromId).add(amount));
            // ToUser nhận tiền -> Giảm số tiền họ được nợ (Giảm balance)
            netBalances.put(toId, netBalances.get(toId).subtract(amount));
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

    private static class UserBalance {
        Long userId;
        BigDecimal balance;

        UserBalance(Long userId, BigDecimal balance) {
            this.userId = userId;
            this.balance = balance;
        }
    }
}