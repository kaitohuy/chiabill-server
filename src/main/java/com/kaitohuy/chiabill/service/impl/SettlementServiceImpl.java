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

        // 2. Lấy toàn bộ dữ liệu Expenses và Approved Payments
        List<Expense> expenses = expenseRepository.fetchAllDataForSettlement(tripId);
        List<Payment> payments = paymentRepository.findByTripIdAndStatus(tripId, PaymentStatus.APPROVED);

        // 3. Map để gom nợ giữa từng cặp (fromId -> toId -> DebtInfo)
        Map<Long, Map<Long, DebtInfo>> debtMap = new HashMap<>();
        Map<Long, String> nameMap = new HashMap<>();

        Map<Long, Boolean> activeMap = tripMemberRepository.findByTripId(tripId).stream()
                .collect(Collectors.toMap(tm -> tm.getUser().getId(), TripMember::getIsActive, (a, b) -> a));

        // 4. Xử lý Expenses (Tính nợ gốc)
        for (Expense expense : expenses) {
            Long payerId = expense.getPayer().getId();
            nameMap.putIfAbsent(payerId, expense.getPayer().getName());

            for (ExpenseSplit split : expense.getSplits()) {
                Long debtorId = split.getUser().getId();
                nameMap.putIfAbsent(debtorId, split.getUser().getName());

                if (!debtorId.equals(payerId)) {
                    addOriginalDebt(debtMap, debtorId, payerId, split.getAmount());
                }
            }
        }

        // 5. Xử lý Payments (Trừ nợ đã trả)
        for (Payment payment : payments) {
            Long fromId = payment.getFromUser().getId();
            Long toId = payment.getToUser().getId();
            addPaidAmount(debtMap, fromId, toId, payment.getAmount());
        }

        // 6. Chốt danh sách cuối cùng
        List<SettlementResponse> result = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, DebtInfo>> fromEntry : debtMap.entrySet()) {
            Long fromId = fromEntry.getKey();
            for (Map.Entry<Long, DebtInfo> toEntry : fromEntry.getValue().entrySet()) {
                Long toId = toEntry.getKey();
                DebtInfo info = toEntry.getValue();

                BigDecimal remaining = info.original.subtract(info.paid).setScale(2, RoundingMode.HALF_UP);

                // Nếu còn nợ (hoặc dư nợ) thì mới hiển thị
                if (remaining.abs().compareTo(new BigDecimal("0.01")) > 0) {
                    SettlementResponse res = new SettlementResponse();
                    res.setFromUserId(fromId);
                    res.setFromUserName(nameMap.get(fromId));
                    res.setToUserId(toId);
                    res.setToUserName(nameMap.get(toId));
                    res.setAmount(remaining);
                    res.setOriginalAmount(info.original);
                    res.setPaidAmount(info.paid);
                    res.setFromUserActive(activeMap.getOrDefault(fromId, false));
                    res.setToUserActive(activeMap.getOrDefault(toId, false));
                    result.add(res);
                }
            }
        }

        return result;
    }

    private void addOriginalDebt(Map<Long, Map<Long, DebtInfo>> map, Long from, Long to, BigDecimal amount) {
        map.computeIfAbsent(from, k -> new HashMap<>())
           .computeIfAbsent(to, k -> new DebtInfo())
           .original = map.get(from).get(to).original.add(amount);
    }

    private void addPaidAmount(Map<Long, Map<Long, DebtInfo>> map, Long from, Long to, BigDecimal amount) {
        map.computeIfAbsent(from, k -> new HashMap<>())
           .computeIfAbsent(to, k -> new DebtInfo())
           .paid = map.get(from).get(to).paid.add(amount);
    }

    private static class DebtInfo {
        BigDecimal original = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
    }
}