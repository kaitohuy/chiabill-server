package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.AppAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppAnnouncementRepository extends JpaRepository<AppAnnouncement, Long> {

    @Query("""
            SELECT a FROM AppAnnouncement a
            WHERE a.isDeleted = false
              AND a.isActive = true
              AND (a.platform = :platformAll OR a.platform = :platform)
              AND (a.startAt IS NULL OR a.startAt <= :now)
              AND (a.endAt IS NULL OR a.endAt >= :now)
            ORDER BY a.priority DESC, a.createdAt DESC
            """)
    List<AppAnnouncement> findActiveAnnouncements(
            @Param("platform") AppAnnouncement.Platform platform,
            @Param("platformAll") AppAnnouncement.Platform platformAll,
            @Param("now") LocalDateTime now
    );

    /** Danh sách tất cả cho admin quản lý (có phân trang) */
    Page<AppAnnouncement> findAllByIsDeletedFalseOrderByPriorityDescCreatedAtDesc(Pageable pageable);
}

