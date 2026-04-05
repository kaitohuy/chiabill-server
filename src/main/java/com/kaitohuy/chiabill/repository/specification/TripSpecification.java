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

    public static Specification<Trip> filter(Long userId, String keyword) {
        return (root, query, cb) -> {
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


            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
