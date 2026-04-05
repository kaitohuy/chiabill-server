package com.kaitohuy.chiabill.repository.specification;

import com.kaitohuy.chiabill.entity.Payment;
import com.kaitohuy.chiabill.entity.PaymentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class PaymentSpecification {

    public static Specification<Payment> filter(Long tripId, PaymentStatus status, Long fromUserId, Long toUserId) {
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
