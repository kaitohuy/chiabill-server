package com.kaitohuy.chiabill.repository.specification;

import com.kaitohuy.chiabill.entity.Trip;
import com.kaitohuy.chiabill.entity.TripMember;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TripSpecification {

    public static Specification<Trip> filter(Long userId, String keyword, Integer month, Integer year) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            // 1. Phải là thành viên của Trip (Dùng Subquery)
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<TripMember> tripMemberRoot = subquery.from(TripMember.class);
            subquery.select(tripMemberRoot.get("trip").get("id"))
                    .where(cb.and(
                            cb.equal(tripMemberRoot.get("user").get("id"), userId),
                            cb.equal(tripMemberRoot.get("isActive"), true)
                    ));
            
            predicates.add(root.get("id").in(subquery));
            predicates.add(cb.equal(root.get("isDeleted"), false));

            // 2. Lọc theo từ khóa (Tên chuyến đi)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }

            // 3. Lọc theo tháng/năm
            if (year != null) {
                java.time.LocalDateTime start;
                java.time.LocalDateTime end;
                if (month != null) {
                    start = java.time.LocalDateTime.of(year, month, 1, 0, 0);
                    end = start.plusMonths(1);
                } else {
                    start = java.time.LocalDateTime.of(year, 1, 1, 0, 0);
                    end = start.plusYears(1);
                }
                
                // Ưu tiên lọc theo startDate, nếu không có thì dùng createdAt
                predicates.add(cb.or(
                    cb.and(cb.isNotNull(root.get("startDate")), cb.greaterThanOrEqualTo(root.get("startDate"), start), cb.lessThan(root.get("startDate"), end)),
                    cb.and(cb.isNull(root.get("startDate")), cb.greaterThanOrEqualTo(root.get("createdAt"), start), cb.lessThan(root.get("createdAt"), end))
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
