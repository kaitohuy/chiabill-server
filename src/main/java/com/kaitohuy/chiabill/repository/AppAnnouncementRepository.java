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

    /**
     * Lấy các thông báo đang active cho client:
     * - is_active = true, is_deleted = false
     * - platform phù hợp (ALL hoặc đúng platform)
     * - Trong khoảng thời gian (start_at <= now <= end_at, hoặc null)
     * Sắp xếp theo priority giảm dần, sau đó mới nhất
     */
    @Query("""
            SELECT a FROM AppAnnouncement a
            WHERE a.isDeleted = false
              AND a.isActive = true
              AND (a.platform = com.kaitohuy.chiabill.entity.AppAnnouncement$Platform.ALL
                   OR a.platform = :platform)
              AND (a.startAt IS NULL OR a.startAt <= :now)
              AND (a.endAt IS NULL OR a.endAt >= :now)
            ORDER BY a.priority DESC, a.createdAt DESC
            """)
    List<AppAnnouncement> findActiveAnnouncements(
            @Param("platform") AppAnnouncement.Platform platform,
            @Param("now") LocalDateTime now
    );

    /**
     * Lấy thông báo UPDATE mới nhất (để check version)
     */
    @Query("""
            SELECT a FROM AppAnnouncement a
            WHERE a.isDeleted = false
              AND a.isActive = true
              AND a.type = com.kaitohuy.chiabill.entity.AppAnnouncement$Type.UPDATE
              AND (a.platform = com.kaitohuy.chiabill.entity.AppAnnouncement$Platform.ALL
                   OR a.platform = :platform)
            ORDER BY a.latestVersion DESC
            LIMIT 1
            """)
    java.util.Optional<AppAnnouncement> findLatestUpdate(
            @Param("platform") AppAnnouncement.Platform platform
    );

    /** Danh sách tất cả cho admin quản lý (có phân trang) */
    Page<AppAnnouncement> findAllByIsDeletedFalseOrderByPriorityDescCreatedAtDesc(Pageable pageable);
}
