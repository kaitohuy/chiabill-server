package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.SystemFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemFeedbackRepository extends JpaRepository<SystemFeedback, Long> {
    Page<SystemFeedback> findAllByIsDeletedFalse(Pageable pageable);
}
