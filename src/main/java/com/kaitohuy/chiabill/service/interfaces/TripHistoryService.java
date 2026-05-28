package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.response.TripHistoryResponse;
import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.entity.Expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.kaitohuy.chiabill.entity.Payment;
import com.kaitohuy.chiabill.entity.Trip;
import java.time.LocalDateTime;
import java.util.List;

public interface TripHistoryService {
    void logAddExpense(User actor, Expense expense);
    void logEditExpense(User actor, Expense oldExpense, Expense newExpense);
    void logDeleteExpense(User actor, Expense expense);
    
    void logAddMember(User actor, Trip trip, User targetUser);
    void logRemoveMember(User actor, Trip trip, User targetUser);
    
    void logPaymentRequest(User actor, Payment payment);
    void logPaymentApprove(User actor, Payment payment);
    void logPaymentReject(User actor, Payment payment);
    void logPayOnBehalf(User payer, Payment payment, List<User> onBehalfOfUsers);

    Page<TripHistoryResponse> getTripHistoryPaginated(Long tripId, List<String> actions, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
