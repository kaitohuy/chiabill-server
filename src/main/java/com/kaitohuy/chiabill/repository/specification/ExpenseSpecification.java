package com.kaitohuy.chiabill.repository.specification;

import com.kaitohuy.chiabill.dto.request.SearchExpenseRequest;
import com.kaitohuy.chiabill.entity.Expense;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ExpenseSpecification {

    public static Specification<Expense> filter(Long tripId, SearchExpenseRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Luôn lọc theo Trip và chưa bị xóa
            predicates.add(cb.equal(root.get("trip").get("id"), tripId));
            predicates.add(cb.equal(root.get("isDeleted"), false));

            // 2. Lọc theo Keyword (Mô tả)
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + request.getKeyword().toLowerCase() + "%"));
            }

            // 3. Lọc theo Category
            if (request.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), request.getCategoryId()));
            }

            // 4. Lọc theo Payer
            if (request.getPayerId() != null) {
                predicates.add(cb.equal(root.get("payer").get("id"), request.getPayerId()));
            }

            // 5. Lọc theo Khoảng ngày
            if (request.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), request.getStartDate()));
            }
            if (request.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), request.getEndDate()));
            }

            // 6. Lọc theo Số tiền
            if (request.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), request.getMinAmount()));
            }
            if (request.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), request.getMaxAmount()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
