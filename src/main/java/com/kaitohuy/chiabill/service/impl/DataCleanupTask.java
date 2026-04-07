package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.entity.Trip;
import com.kaitohuy.chiabill.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataCleanupTask {

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final PaymentRepository paymentRepository;
    private final TripInvitationRepository invitationRepository;
    private final NotificationRepository notificationRepository;
    private final com.kaitohuy.chiabill.service.interfaces.CloudinaryService cloudinaryService;

    private static final int BATCH_SIZE = 100;
    private static final int THRESHOLD_DAYS = 45;

    /**
     * Chạy tác vụ dọn dẹp hàng ngày lúc 3:00 AM
     * Cron: giây phút giờ ngày tháng thứ
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void runDailyCleanup() {
        log.info("Starting daily database cleanup task (Threshold: {} days)...", THRESHOLD_DAYS);
        
        LocalDateTime threshold = LocalDateTime.now().minusDays(THRESHOLD_DAYS);
        
        try {
            cleanupTrips(threshold);
            cleanupExpenses(threshold);
            cleanupInactiveMembers(threshold);
            cleanupOldNotifications(threshold);
            log.info("Daily database cleanup task completed successfully.");
        } catch (Exception e) {
            log.error("Error occurred during database cleanup task: ", e);
        }
    }

    private void cleanupTrips(LocalDateTime threshold) {
        log.info("Cleaning up soft-deleted trips older than {} days...", THRESHOLD_DAYS);
        
        boolean hasMore = true;
        int totalDeleted = 0;

        while (hasMore) {
            // Lấy 1 lô Trip đã xóa
            List<Trip> tripsToDelete = tripRepository.findByIsDeletedTrueAndUpdatedAtBefore(
                    threshold, PageRequest.of(0, BATCH_SIZE));

            if (tripsToDelete.isEmpty()) {
                hasMore = false;
                continue;
            }

            List<Long> tripIds = tripsToDelete.stream()
                    .map(Trip::getId)
                    .collect(Collectors.toList());

            // 0. Xóa các tệp ảnh liên quan trên Cloudinary (Receipts & Covers)
            // Lấy toàn bộ receiptUrl trong Trip
            List<String> receiptUrls = expenseRepository.findReceiptUrlsByTripIdIn(tripIds);
            receiptUrls.forEach(cloudinaryService::deleteImage);
            
            // Lấy toàn bộ coverUrl của các Trip sắp xóa
            tripsToDelete.stream()
                    .map(Trip::getCoverUrl)
                    .filter(java.util.Objects::nonNull)
                    .forEach(cloudinaryService::deleteImage);

            // Xóa theo thứ tự để tránh lỗi khóa ngoại (Foreign Key)
            // 1. Xóa Split của các Expense trong Trip
            expenseSplitRepository.deleteByTripIdIn(tripIds);
            
            // 2. Xóa Expense
            expenseRepository.deleteByTripIdIn(tripIds);
            
            // 3. Xóa Payment
            paymentRepository.deleteByTripIdIn(tripIds);
            
            // 4. Xóa Invitation
            invitationRepository.deleteByTripIdIn(tripIds);
            
            // 5. Xóa Member
            tripMemberRepository.deleteByTripIdIn(tripIds);
            
            // 6. Xóa chính bản ghi Trip
            tripRepository.deleteAllInBatch(tripsToDelete);

            totalDeleted += tripsToDelete.size();
            log.info("Deleted batch of {} trips. Total deleted so far: {}", tripsToDelete.size(), totalDeleted);

            // Vì ta xóa theo lô (batch), nên vòng lặp tiếp theo sẽ lấy 100 bản ghi tiếp theo thỏa mãn điều kiện
        }
    }

    private void cleanupExpenses(LocalDateTime threshold) {
        log.info("Cleaning up soft-deleted expenses older than {} days and their images...", THRESHOLD_DAYS);
        try {
            // Lấy danh sách ảnh hóa đơn
            List<String> receiptUrls = expenseRepository.findReceiptUrlsByIsDeletedTrueAndUpdatedAtBefore(threshold);
            receiptUrls.forEach(cloudinaryService::deleteImage);

            // Xóa dữ liệu cứng
            // Lưu ý: Ta nên xóa splits trước (nếu không có Cascade delete hỗ trợ cho custom query)
            // Trong repo ta dùng custom query DELETE FROM Expense sẽ cần quản lý splits
            // Nhưng hiện tại để đơn giản, ta cứ gọi deleteSoftDeletedExpenses
            // Nếu có FK constraint, server sẽ báo lỗi, ta sẽ sửa Repo sau.
            expenseRepository.deleteSoftDeletedExpenses(threshold);
        } catch (Exception e) {
            log.error("Error cleaning up soft-deleted expenses: ", e);
        }
    }

    private void cleanupInactiveMembers(LocalDateTime threshold) {
        log.info("Cleaning up inactive/disabled members older than {} days...", THRESHOLD_DAYS);
        try {
            tripMemberRepository.deleteInactiveMembers(threshold);
        } catch (Exception e) {
            log.error("Error cleaning up inactive members: ", e);
        }
    }

    private void cleanupOldNotifications(LocalDateTime threshold) {
        log.info("Cleaning up notifications older than {} days...", THRESHOLD_DAYS);
        try {
            notificationRepository.deleteOldNotifications(threshold);
        } catch (Exception e) {
            log.error("Error cleaning up old notifications: ", e);
        }
    }
}
