package com.kaitohuy.chiabill.service.impl;

import com.google.firebase.messaging.*;
import com.kaitohuy.chiabill.dto.response.NotificationResponse;
import com.kaitohuy.chiabill.entity.Notification;
import com.kaitohuy.chiabill.entity.NotificationType;
import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.entity.UserDeviceToken;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.mapper.NotificationMapper;
import com.kaitohuy.chiabill.repository.NotificationRepository;
import com.kaitohuy.chiabill.repository.UserDeviceTokenRepository;
import com.kaitohuy.chiabill.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserDeviceTokenRepository tokenRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public void registerToken(User user, String token, String platform) {
        tokenRepository.findByToken(token).ifPresentOrElse(
                deviceToken -> {
                    deviceToken.setUser(user);
                    deviceToken.setPlatform(platform);
                    deviceToken.setLastUsedAt(LocalDateTime.now());
                    tokenRepository.save(deviceToken);
                },
                () -> {
                    UserDeviceToken newToken = UserDeviceToken.builder()
                            .user(user)
                            .token(token)
                            .platform(platform)
                            .lastUsedAt(LocalDateTime.now())
                            .build();
                    tokenRepository.save(newToken);
                }
        );
    }

    @Override
    @Transactional
    public void sendNotification(User receiver, String title, String body, NotificationType type, Long referenceId) {
        Notification notification = Notification.builder()
                .user(receiver)
                .title(title)
                .message(body)
                .type(type)
                .referenceId(referenceId)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        // 2. Fetch device tokens
        List<UserDeviceToken> tokens = tokenRepository.findByUserId(receiver.getId());
        if (tokens.isEmpty()) {
            log.info("No device tokens found for user: {}", receiver.getEmail());
            return;
        }

        // 3. Send via FCM
        for (UserDeviceToken deviceToken : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(deviceToken.getToken())
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("type", type.name())
                        .putData("referenceId", String.valueOf(referenceId))
                        .build();

                FirebaseMessaging.getInstance().send(message);
                log.info("Notification sent successfully to user {} on device {}", receiver.getEmail(), deviceToken.getPlatform());
            } catch (FirebaseMessagingException e) {
                log.error("Failed to send notification to device {}: {}", deviceToken.getToken(), e.getMessage());
                if ("registration-token-not-registered".equals(e.getMessagingErrorCode().name().toLowerCase())) {
                   tokenRepository.delete(deviceToken);
                   log.info("Deleted invalid token: {}", deviceToken.getToken());
                }
            }
        }
    }

    @Override
    public List<NotificationResponse> getNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notificationMapper.toResponseList(notifications);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("Notification not found"));
        
        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException("Access denied");
        }
        
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}
