package com.kaitohuy.chiabill.scheduler;

import com.kaitohuy.chiabill.entity.NotificationType;
import com.kaitohuy.chiabill.entity.ScheduledNotification;
import com.kaitohuy.chiabill.repository.ScheduledNotificationRepository;
import com.kaitohuy.chiabill.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledNotificationScheduler {

    private final ScheduledNotificationRepository scheduledNotificationRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processScheduledNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledNotification> pendingNotifications = scheduledNotificationRepository
                .findByScheduledTimeBeforeAndIsSentFalseAndIsDeletedFalse(now);

        if (pendingNotifications.isEmpty()) {
            return;
        }

        log.info("Found {} pending scheduled notifications to send at {}", pendingNotifications.size(), now);

        for (ScheduledNotification notification : pendingNotifications) {
            try {
                // Send push notification using the existing NotificationService
                notificationService.sendNotification(
                        notification.getUser(),
                        notification.getTitle(),
                        notification.getMessage(),
                        NotificationType.ITINERARY,
                        notification.getTrip().getId()
                );
                
                notification.setIsSent(true);
                scheduledNotificationRepository.save(notification);
                log.info("Successfully processed and sent scheduled notification ID {} for user {}", 
                        notification.getId(), notification.getUser().getId());
            } catch (Exception e) {
                log.error("Failed to process scheduled notification ID {}: {}", notification.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanUpOldScheduledNotifications() {
        log.info("Starting scheduled notification cleanup...");
        try {
            scheduledNotificationRepository.cleanUpSentOrDeleted();
            log.info("Successfully cleaned up sent or deleted scheduled notifications.");
        } catch (Exception e) {
            log.error("Failed to clean up scheduled notifications: {}", e.getMessage());
        }
    }
}
