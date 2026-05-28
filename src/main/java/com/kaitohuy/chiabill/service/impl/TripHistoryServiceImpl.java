package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.response.TripHistoryResponse;
import com.kaitohuy.chiabill.entity.Expense;
import com.kaitohuy.chiabill.entity.TripHistory;
import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.repository.TripHistoryRepository;
import com.kaitohuy.chiabill.service.interfaces.TripHistoryService;
import com.kaitohuy.chiabill.utils.CurrencyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripHistoryServiceImpl implements TripHistoryService {

    private final TripHistoryRepository tripHistoryRepository;

    @Override
    public void logEditExpense(User actor, Expense oldExpense, Expense newExpense) {
        List<String> changes = new ArrayList<>();

        // Kiểm tra thay đổi số tiền
        if (oldExpense.getTotalAmount().compareTo(newExpense.getTotalAmount()) != 0) {
            changes.add(String.format("Sửa số tiền từ %s thành %s", 
                CurrencyUtil.format(oldExpense.getTotalAmount()), 
                CurrencyUtil.format(newExpense.getTotalAmount())));
        }

        // Kiểm tra thay đổi danh mục
        if (!Objects.equals(oldExpense.getCategory().getId(), newExpense.getCategory().getId())) {
            changes.add(String.format("Đổi danh mục từ '%s' sang '%s'", 
                oldExpense.getCategory().getName(), 
                newExpense.getCategory().getName()));
        }

        // Kiểm tra thay đổi mô tả
        if (!Objects.equals(oldExpense.getDescription(), newExpense.getDescription())) {
            changes.add(String.format("Sửa mô tả từ '%s' thành '%s'", 
                oldExpense.getDescription(), 
                newExpense.getDescription()));
        }

        // Kiểm tra thay đổi người chi
        if (!Objects.equals(oldExpense.getPayer().getId(), newExpense.getPayer().getId())) {
            changes.add(String.format("Đổi người chi từ %s sang %s", 
                oldExpense.getPayer().getName(), 
                newExpense.getPayer().getName()));
        }

        if (changes.isEmpty()) {
            return;
        }

        String content = String.join(", ", changes);
        TripHistory history = TripHistory.builder()
                .tripId(oldExpense.getTrip().getId())
                .actor(actor)
                .action("EDIT_EXPENSE")
                .content(content)
                .build();
        
        tripHistoryRepository.save(history);
    }

    @Override
    public void logDeleteExpense(User actor, Expense expense) {
        String content = String.format("Xoá khoản chi '%s' trị giá %s", 
            expense.getCategory().getName(), 
            CurrencyUtil.format(expense.getTotalAmount()));
        
        TripHistory history = TripHistory.builder()
                .tripId(expense.getTrip().getId())
                .actor(actor)
                .action("DELETE_EXPENSE")
                .content(content)
                .build();
        
        tripHistoryRepository.save(history);
    }

    @Override
    public void logAddExpense(User actor, Expense expense) {
        String content = String.format("Thêm khoản chi '%s' trị giá %s",
            expense.getCategory().getName(),
            CurrencyUtil.format(expense.getTotalAmount()));

        TripHistory history = TripHistory.builder()
                .tripId(expense.getTrip().getId())
                .actor(actor)
                .action("ADD_EXPENSE")
                .content(content)
                .build();

        tripHistoryRepository.save(history);
    }

    @Override
    public void logAddMember(User actor, com.kaitohuy.chiabill.entity.Trip trip, User targetUser) {
        String content = String.format("Thêm %s vào nhóm", targetUser.getName());

        TripHistory history = TripHistory.builder()
                .tripId(trip.getId())
                .actor(actor)
                .action("ADD_MEMBER")
                .content(content)
                .build();

        tripHistoryRepository.save(history);
    }

    @Override
    public void logRemoveMember(User actor, com.kaitohuy.chiabill.entity.Trip trip, User targetUser) {
        String content = String.format("Xóa %s khỏi nhóm", targetUser.getName());

        TripHistory history = TripHistory.builder()
                .tripId(trip.getId())
                .actor(actor)
                .action("REMOVE_MEMBER")
                .content(content)
                .build();

        tripHistoryRepository.save(history);
    }

    @Override
    public void logPaymentRequest(User actor, com.kaitohuy.chiabill.entity.Payment payment) {
        String content = String.format("Yêu cầu duyệt khoản thanh toán %s cho %s",
            CurrencyUtil.format(payment.getAmount()),
            payment.getToUser().getName());

        TripHistory history = TripHistory.builder()
                .tripId(payment.getTrip().getId())
                .actor(actor)
                .action("PAYMENT_REQUEST")
                .content(content)
                .build();

        tripHistoryRepository.save(history);
    }

    @Override
    public void logPaymentApprove(User actor, com.kaitohuy.chiabill.entity.Payment payment) {
        String content = String.format("Đã duyệt khoản thanh toán %s từ %s",
            CurrencyUtil.format(payment.getAmount()),
            payment.getFromUser().getName());

        TripHistory history = TripHistory.builder()
                .tripId(payment.getTrip().getId())
                .actor(actor)
                .action("PAYMENT_APPROVE")
                .content(content)
                .build();

        tripHistoryRepository.save(history);
    }

    @Override
    public void logPaymentReject(User actor, com.kaitohuy.chiabill.entity.Payment payment) {
        String content = String.format("Đã từ chối khoản thanh toán %s từ %s",
            CurrencyUtil.format(payment.getAmount()),
            payment.getFromUser().getName());

        TripHistory history = TripHistory.builder()
                .tripId(payment.getTrip().getId())
                .actor(actor)
                .action("PAYMENT_REJECT")
                .content(content)
                .build();

        tripHistoryRepository.save(history);
    }

    @Override
    public void logPayOnBehalf(User payer, com.kaitohuy.chiabill.entity.Payment payment, List<com.kaitohuy.chiabill.entity.User> onBehalfOfUsers) {
        String names = onBehalfOfUsers.stream()
                .map(com.kaitohuy.chiabill.entity.User::getName)
                .collect(java.util.stream.Collectors.joining(", "));
        String content = String.format("%s đã thanh toán hộ [%s] cho %s tổng cộng %s",
                payer.getName(), names, payment.getToUser().getName(), CurrencyUtil.format(payment.getAmount()));

        TripHistory history = TripHistory.builder()
                .tripId(payment.getTrip().getId())
                .actor(payer)
                .action("PAYMENT_ON_BEHALF")
                .content(content)
                .build();
        tripHistoryRepository.save(history);
    }

    @Override
    public org.springframework.data.domain.Page<TripHistoryResponse> getTripHistoryPaginated(Long tripId, List<String> actions, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, org.springframework.data.domain.Pageable pageable) {
        return tripHistoryRepository.findFilteredHistories(tripId, actions, startDate, endDate, pageable)
                .map(this::mapToResponse);
    }

    private TripHistoryResponse mapToResponse(TripHistory history) {
        return TripHistoryResponse.builder()
                .id(history.getId())
                .tripId(history.getTripId())
                .actorId(history.getActor().getId())
                .actorName(history.getActor().getName())
                .action(history.getAction())
                .content(history.getContent())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
