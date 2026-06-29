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

        // Determine receiver language and translate title and body
        String language = receiverUser.getLanguage();
        String finalTitle = translateTitle(title, type, language);
        String finalBody = translateBody(body, type, language);

        Notification notification = Notification.builder()
                .user(receiverUser)
                .title(finalTitle)
                .message(finalBody)
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
                        .putData("title", finalTitle)
                        .putData("body", finalBody)
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
                                    .setTitle(finalTitle)
                                    .setBody(finalBody);
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

    private String translateTitle(String title, NotificationType type, String lang) {
        if (!"en".equalsIgnoreCase(lang) || title == null) {
            return title;
        }
        if ("Tạm ngưng hoạt động".equals(title)) {
            return "Activity Suspended";
        }
        if (title.startsWith("Bạn nhận được thanh toán: ")) {
            return "Payment Received: " + title.substring("Bạn nhận được thanh toán: ".length());
        }
        if (title.startsWith("Thanh toán được phê duyệt: ")) {
            return "Payment Approved: " + title.substring("Thanh toán được phê duyệt: ".length());
        }
        if (title.startsWith("Thanh toán bị từ chối: ")) {
            return "Payment Rejected: " + title.substring("Thanh toán bị từ chối: ".length());
        }
        if (title.startsWith("Thanh toán hộ: ")) {
            return "Pay on Behalf: " + title.substring("Thanh toán hộ: ".length());
        }
        if (title.startsWith("Khoản chi mới: ")) {
            return "New Expense: " + title.substring("Khoản chi mới: ".length());
        }
        if (title.startsWith("Sắp đến lịch trình: ")) {
            return "Upcoming Itinerary: " + title.substring("Sắp đến lịch trình: ".length());
        }
        return title;
    }

    private String translateBody(String body, NotificationType type, String lang) {
        if (!"en".equalsIgnoreCase(lang) || body == null) {
            return body;
        }
        
        // 1. MEMBER_DISABLED
        if (type == NotificationType.MEMBER_DISABLED && body.startsWith("Bạn vừa bị ")) {
            int idx1 = body.indexOf(" tạm ngưng hoạt động trong chuyến đi ");
            if (idx1 != -1) {
                String actor = body.substring("Bạn vừa bị ".length(), idx1);
                int idx2 = body.indexOf(", hãy mau chóng làm lành");
                if (idx2 != -1) {
                    String trip = body.substring(idx1 + " tạm ngưng hoạt động trong chuyến đi ".length(), idx2);
                    return "You have been temporarily suspended from the trip \"" + trip + "\" by " + actor + ". Let's make peace soon!";
                }
            }
        }
        
        // 2. PAYMENT_REQUESTED
        if (type == NotificationType.PAYMENT_REQUESTED) {
            int idx1 = body.indexOf(" đã gửi ");
            int idx2 = body.indexOf(" cho bạn. Trạng thái: ");
            if (idx1 != -1 && idx2 != -1) {
                String payer = body.substring(0, idx1);
                String amount = body.substring(idx1 + " đã gửi ".length(), idx2);
                String status = body.substring(idx2 + " cho bạn. Trạng thái: ".length());
                if ("PENDING".equals(status)) status = "Pending";
                if ("APPROVED".equals(status)) status = "Approved";
                if ("REJECTED".equals(status)) status = "Rejected";
                return payer + " has sent you " + amount + ". Status: " + status;
            }
            
            int idx3 = body.indexOf(" đã thanh toán hộ cho [");
            int idx4 = body.indexOf("] tổng cộng ");
            if (idx3 != -1 && idx4 != -1) {
                String payer = body.substring(0, idx3);
                String names = body.substring(idx3 + " đã thanh toán hộ cho [".length(), idx4);
                String amount = body.substring(idx4 + "] tổng cộng ".length());
                return payer + " paid on behalf of [" + names + "] for a total of " + amount;
            }
        }
        
        // 3. PAYMENT_APPROVED
        if (type == NotificationType.PAYMENT_APPROVED && body.contains(" đã xác nhận nhận được số tiền ")) {
            int idx = body.indexOf(" đã xác nhận nhận được số tiền ");
            if (idx != -1) {
                String verifier = body.substring(0, idx);
                String amount = body.substring(idx + " đã xác nhận nhận được số tiền ".length());
                return verifier + " has confirmed receiving the payment of " + amount;
            }
        }
        
        // 4. SYSTEM
        if (type == NotificationType.SYSTEM && body.contains(" đã từ chối xác nhận số tiền ") && body.contains(". Vui lòng kiểm tra lại.")) {
            int idx1 = body.indexOf(" đã từ chối xác nhận số tiền ");
            int idx2 = body.indexOf(". Vui lòng kiểm tra lại.");
            if (idx1 != -1 && idx2 != -1) {
                String verifier = body.substring(0, idx1);
                String amount = body.substring(idx1 + " đã từ chối xác nhận số tiền ".length(), idx2);
                return verifier + " has rejected the payment of " + amount + ". Please double check.";
            }
        }
        
        // 5. EXPENSE_CREATED
        if (type == NotificationType.EXPENSE_CREATED && body.contains(" vừa thêm ") && body.contains(" cho ")) {
            int idx1 = body.indexOf(" vừa thêm ");
            int idx2 = body.indexOf(" cho ");
            if (idx1 != -1 && idx2 != -1) {
                String payer = body.substring(0, idx1);
                String amount = body.substring(idx1 + " vừa thêm ".length(), idx2);
                String category = body.substring(idx2 + " cho ".length());
                if ("Ăn uống".equalsIgnoreCase(category)) category = "Food & Beverage";
                else if ("Di chuyển".equalsIgnoreCase(category)) category = "Transport";
                else if ("Lưu trú".equalsIgnoreCase(category)) category = "Accommodation";
                else if ("Vui chơi".equalsIgnoreCase(category)) category = "Entertainment";
                else if ("Mua sắm".equalsIgnoreCase(category)) category = "Shopping";
                else if ("Khác".equalsIgnoreCase(category)) category = "Others";
                return payer + " just added " + amount + " for " + category;
            }
        }
        
        // 6. ITINERARY
        if (type == NotificationType.ITINERARY && body.startsWith("Thời gian diễn ra: ") && body.contains(" (Báo trước ")) {
            int idx1 = body.indexOf(" (Báo trước ");
            if (idx1 != -1) {
                String timeRange = body.substring("Thời gian diễn ra: ".length(), idx1);
                String rest = body.substring(idx1 + " (Báo trước ".length());
                if (rest.endsWith(")")) {
                    String alarmDetail = rest.substring(0, rest.length() - 1);
                    String[] alarmParts = alarmDetail.split(" ");
                    if (alarmParts.length >= 2) {
                        String val = alarmParts[0];
                        String unit = alarmParts[1];
                        if ("Phút".equalsIgnoreCase(unit)) unit = "minutes";
                        else if ("Giờ".equalsIgnoreCase(unit)) unit = "hours";
                        else if ("Ngày".equalsIgnoreCase(unit)) unit = "days";
                        return "Time: " + timeRange + " (Reminded " + val + " " + unit + " in advance)";
                    }
                }
            }
        }
        
        return body;
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
