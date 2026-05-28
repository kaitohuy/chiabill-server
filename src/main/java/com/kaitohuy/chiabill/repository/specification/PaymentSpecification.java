package com.kaitohuy.chiabill.repository.specification;

import com.kaitohuy.chiabill.entity.Payment;
import com.kaitohuy.chiabill.entity.PaymentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;

public class PaymentSpecification {

    public static Specification<Payment> filter(Long tripId, PaymentStatus status, Long fromUserId, Long toUserId, LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Phải thuộc Trip
            predicates.add(cb.equal(root.get("trip").get("id"), tripId));

            // 2. Lọc theo trạng thái (PENDING, APPROVED, REJECTED)
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 3. Lọc theo người gửi (Dòng tiền đi)
            if (fromUserId != null) {
                predicates.add(cb.equal(root.get("fromUser").get("id"), fromUserId));
            }

            // 4. Lọc theo người nhận (Dòng tiền đến)
            if (toUserId != null) {
                predicates.add(cb.equal(root.get("toUser").get("id"), toUserId));
            }

            // 5. Lọc theo khoảng thời gian
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
