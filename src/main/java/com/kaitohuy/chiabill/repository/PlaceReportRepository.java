package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.PlaceReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceReportRepository extends JpaRepository<PlaceReport, Long> {
    org.springframework.data.domain.Page<PlaceReport> findAllByIsDeletedFalse(org.springframework.data.domain.Pageable pageable);
}
