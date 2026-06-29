package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.entity.Notification;
import com.kaitohuy.chiabill.entity.NotificationType;
import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.repository.NotificationRepository;
import com.kaitohuy.chiabill.repository.TripRepository;
import com.kaitohuy.chiabill.repository.UserDeviceTokenRepository;
import com.kaitohuy.chiabill.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserDeviceTokenRepository tokenRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User receiver;

    @BeforeEach
    void setUp() {
        receiver = new User();
        receiver.setId(1L);
        receiver.setName("Nguyễn Văn A");
        receiver.setLanguage("vi");
    }

    @Test
    void sendNotification_WhenLanguageIsVi_DoesNotTranslate() {
        // GIVEN
        receiver.setLanguage("vi");
        when(userRepository.findById(1L)).thenReturn(Optional.of(receiver));
        when(tokenRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        // WHEN
        notificationService.sendNotification(
                receiver,
                "Khoản chi mới: Đà Lạt 2026",
                "Huy vừa thêm 150,000 đ cho Ăn uống",
                NotificationType.EXPENSE_CREATED,
                100L
        );

        // THEN
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        
        Notification saved = captor.getValue();
        assertEquals("Khoản chi mới: Đà Lạt 2026", saved.getTitle());
        assertEquals("Huy vừa thêm 150,000 đ cho Ăn uống", saved.getMessage());
    }

    @Test
    void sendNotification_WhenLanguageIsEn_TranslatesExpenseCreated() {
        // GIVEN
        receiver.setLanguage("en");
        when(userRepository.findById(1L)).thenReturn(Optional.of(receiver));
        when(tokenRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        // WHEN
        notificationService.sendNotification(
                receiver,
                "Khoản chi mới: Đà Lạt 2026",
                "Huy vừa thêm 150,000 đ cho Ăn uống",
                NotificationType.EXPENSE_CREATED,
                100L
        );

        // THEN
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals("New Expense: Đà Lạt 2026", saved.getTitle());
        assertEquals("Huy just added 150,000 đ for Food & Beverage", saved.getMessage());
    }

    @Test
    void sendNotification_WhenLanguageIsEn_TranslatesPaymentRequestedAndApproved() {
        // GIVEN
        receiver.setLanguage("en");
        when(userRepository.findById(1L)).thenReturn(Optional.of(receiver));
        when(tokenRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        // 1. Payment requested standard
        notificationService.sendNotification(
                receiver,
                "Bạn nhận được thanh toán: Vũng Tàu",
                "Huy đã gửi 200,000 đ cho bạn. Trạng thái: PENDING",
                NotificationType.PAYMENT_REQUESTED,
                100L
        );

        // 2. Payment requested pay on behalf
        notificationService.sendNotification(
                receiver,
                "Thanh toán hộ: Vũng Tàu",
                "Huy đã thanh toán hộ cho [An, Bình] tổng cộng 500,000 đ",
                NotificationType.PAYMENT_REQUESTED,
                100L
        );

        // 3. Payment approved
        notificationService.sendNotification(
                receiver,
                "Thanh toán được phê duyệt: Vũng Tàu",
                "Huy đã xác nhận nhận được số tiền 200,000 đ",
                NotificationType.PAYMENT_APPROVED,
                100L
        );

        // 4. Payment rejected (system)
        notificationService.sendNotification(
                receiver,
                "Thanh toán bị từ chối: Vũng Tàu",
                "Huy đã từ chối xác nhận số tiền 200,000 đ. Vui lòng kiểm tra lại.",
                NotificationType.SYSTEM,
                100L
        );

        // THEN
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(4)).save(captor.capture());

        assertEquals("Payment Received: Vũng Tàu", captor.getAllValues().get(0).getTitle());
        assertEquals("Huy has sent you 200,000 đ. Status: Pending", captor.getAllValues().get(0).getMessage());

        assertEquals("Pay on Behalf: Vũng Tàu", captor.getAllValues().get(1).getTitle());
        assertEquals("Huy paid on behalf of [An, Bình] for a total of 500,000 đ", captor.getAllValues().get(1).getMessage());

        assertEquals("Payment Approved: Vũng Tàu", captor.getAllValues().get(2).getTitle());
        assertEquals("Huy has confirmed receiving the payment of 200,000 đ", captor.getAllValues().get(2).getMessage());

        assertEquals("Payment Rejected: Vũng Tàu", captor.getAllValues().get(3).getTitle());
        assertEquals("Huy has rejected the payment of 200,000 đ. Please double check.", captor.getAllValues().get(3).getMessage());
    }

    @Test
    void sendNotification_WhenLanguageIsEn_TranslatesItineraryAndMemberDisabled() {
        // GIVEN
        receiver.setLanguage("en");
        when(userRepository.findById(1L)).thenReturn(Optional.of(receiver));
        when(tokenRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        // 1. Itinerary alarm
        notificationService.sendNotification(
                receiver,
                "Sắp đến lịch trình: Ăn tối hải sản",
                "Thời gian diễn ra: 18:00 - 20:00 (Báo trước 30 Phút)",
                NotificationType.ITINERARY,
                100L
        );

        // 2. Member disabled
        notificationService.sendNotification(
                receiver,
                "Tạm ngưng hoạt động",
                "Bạn vừa bị Huy tạm ngưng hoạt động trong chuyến đi Phú Quốc, hãy mau chóng làm lành đi nhé!",
                NotificationType.MEMBER_DISABLED,
                100L
        );

        // THEN
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());

        assertEquals("Upcoming Itinerary: Ăn tối hải sản", captor.getAllValues().get(0).getTitle());
        assertEquals("Time: 18:00 - 20:00 (Reminded 30 minutes in advance)", captor.getAllValues().get(0).getMessage());

        assertEquals("Activity Suspended", captor.getAllValues().get(1).getTitle());
        assertEquals("You have been temporarily suspended from the trip \"Phú Quốc\" by Huy. Let's make peace soon!", captor.getAllValues().get(1).getMessage());
    }
}
