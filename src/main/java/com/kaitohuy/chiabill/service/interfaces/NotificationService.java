package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.response.NotificationResponse;
import com.kaitohuy.chiabill.entity.NotificationType;
import com.kaitohuy.chiabill.entity.User;

import java.util.List;

public interface NotificationService {

    void registerToken(User user, String token, String platform);

    void sendNotification(User receiver, String title, String body, NotificationType type, Long referenceId);

    List<NotificationResponse> getNotifications(Long userId);

    void markAsRead(Long notificationId, Long userId);
    
    long getUnreadCount(Long userId);
}
