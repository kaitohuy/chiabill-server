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
import org.springframework.scheduling.annotation.Async;
import java.util.List;

import com.kaitohuy.chiabill.repository.TripRepository;
import com.kaitohuy.chiabill.entity.Trip;
import com.kaitohuy.chiabill.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserDeviceTokenRepository tokenRepository;
    private final NotificationMapper notificationMapper;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

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
    @Async
    @Transactional
    public void sendNotification(User receiver, String title, String body, NotificationType type, Long referenceId) {
        User receiverUser = userRepository.findById(receiver.getId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy người dùng"));

        Notification notification = Notification.builder()
                .user(receiverUser)
                .title(title)
                .message(body)
                .type(type)
                .referenceId(referenceId)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        // 2. Fetch device tokens
        List<UserDeviceToken> tokens = tokenRepository.findByUserId(receiverUser.getId());
        if (tokens.isEmpty()) {
            log.info("No device tokens found for user ID: {} ({})", receiverUser.getId(), receiverUser.getName());
            return;
        }

        // Lấy coverUrl từ chuyến đi (nếu có)
        String imageUrl = null;
        if (referenceId != null && (type == NotificationType.ITINERARY 
                || type == NotificationType.EXPENSE_CREATED 
                || type == NotificationType.PAYMENT_REQUESTED 
                || type == NotificationType.PAYMENT_APPROVED)) {
            imageUrl = tripRepository.findById(referenceId)
                    .map(Trip::getCoverUrl)
                    .orElse(null);
        }

        // 3. Send via FCM
        for (UserDeviceToken deviceToken : tokens) {
            try {
                boolean isAndroid = "ANDROID".equalsIgnoreCase(deviceToken.getPlatform());

                Message.Builder messageBuilder = Message.builder()
                        .setToken(deviceToken.getToken())
                        .putData("title", title)
                        .putData("body", body)
                        .putData("type", type.name())
                        .putData("referenceId", String.valueOf(referenceId));

                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    messageBuilder.putData("imageUrl", imageUrl);
                }

                if (isAndroid) {
                    AndroidConfig androidConfig = AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .build();
                    messageBuilder.setAndroidConfig(androidConfig);
                } else {
                    AndroidConfig androidConfig = AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId("high_importance_channel")
                                    .setPriority(AndroidNotification.Priority.HIGH)
                                    .setVisibility(AndroidNotification.Visibility.PUBLIC)
                                    .setDefaultSound(true)
                                    .setColor("#10B981") // Màu xanh Emerald đặc trưng của app
                                    .build())
                            .build();

                    ApnsConfig apnsConfig = ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setSound("default")
                                    .setContentAvailable(true)
                                    .build())
                            .build();

                    com.google.firebase.messaging.Notification.Builder notificationBuilder = 
                            com.google.firebase.messaging.Notification.builder()
                                    .setTitle(title)
                                    .setBody(body);
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        notificationBuilder.setImage(imageUrl);
                    }

                    messageBuilder
                            .setNotification(notificationBuilder.build())
                            .setAndroidConfig(androidConfig)
                            .setApnsConfig(apnsConfig);
                }

                Message message = messageBuilder.build();
                FirebaseMessaging.getInstance().send(message);
                log.info("Notification sent successfully to user ID: {} ({}) on device {}", receiverUser.getId(), receiverUser.getName(), deviceToken.getPlatform());
            } catch (FirebaseMessagingException e) {
                log.error("Failed to send notification to device {}: {}", deviceToken.getToken(), e.getMessage());
                // UNREGISTERED = token không còn hợp lệ (app gỡ cài, token hết hạn...)
                // → Xoá khỏi DB luôn để tránh gửi lại lần sau
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    tokenRepository.delete(deviceToken);
                    log.info("Deleted invalid/expired FCM token for user ID: {}", receiverUser.getId());
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
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}
