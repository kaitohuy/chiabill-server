package com.kaitohuy.chiabill.repository;

import com.kaitohuy.chiabill.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndIsReadFalse(Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Notification n WHERE n.updatedAt < :threshold")
    void deleteOldNotifications(@org.springframework.data.repository.query.Param("threshold") java.time.LocalDateTime threshold);
}
