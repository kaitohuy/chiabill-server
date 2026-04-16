package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.dto.response.PageResponse;
import com.kaitohuy.chiabill.dto.response.PaymentResponse;
import com.kaitohuy.chiabill.entity.*;
import com.kaitohuy.chiabill.exception.BusinessException;
import com.kaitohuy.chiabill.mapper.PaymentMapper;
import com.kaitohuy.chiabill.repository.*;
import com.kaitohuy.chiabill.repository.specification.PaymentSpecification;
import com.kaitohuy.chiabill.service.interfaces.CloudinaryService;
import com.kaitohuy.chiabill.service.interfaces.NotificationService;
import com.kaitohuy.chiabill.utils.CurrencyUtil;

import com.kaitohuy.chiabill.service.interfaces.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripMemberRepository tripMemberRepository;
    private final CloudinaryService cloudinaryService;
    private final NotificationService notificationService;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponse createPayment(Long tripId, Long fromUserId, Long toUserId, BigDecimal amount, MultipartFile proof) {

        if (amount.compareTo(new BigDecimal("9999999999999")) > 0) {
            throw new BusinessException("Số tiền thanh toán quá lớn, vui lòng kiểm tra lại");
        }
        
        // Kiểm tra tư cách thành viên
        if (!tripMemberRepository.existsByTripIdAndUserId(tripId, fromUserId)) {
            throw new BusinessException("Bạn không thuộc chuyến đi này");
        }
        if (!tripMemberRepository.existsByTripIdAndUserId(tripId, toUserId)) {
            throw new BusinessException("Người nhận không thuộc chuyến đi này");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));
        
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new BusinessException("User not found"));
        
        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Upload minh chứng lên Cloudinary
        String proofUrl = cloudinaryService.uploadImage(proof);

        // Xác định trạng thái ban đầu dựa trên cấu hình của người nhận
        PaymentStatus initialStatus = Boolean.TRUE.equals(toUser.getAllowAutoApprovePayment())
                ? PaymentStatus.APPROVED
                : PaymentStatus.PENDING;

        // Lưu bản ghi thanh toán
        Payment payment = Payment.builder()
                .trip(trip)
                .fromUser(fromUser)
                .toUser(toUser)
                .amount(amount)
                .proofUrl(proofUrl)
                .status(initialStatus)
                .build();

        paymentRepository.save(payment);

        // 🚀 Bắn thông báo cho người nhận tiền
        try {
            notificationService.sendNotification(
                    toUser,
                    "Bạn nhận được thanh toán: " + trip.getName(),
                    fromUser.getName() + " đã gửi " + CurrencyUtil.format(amount) + " cho bạn. Trạng thái: " + initialStatus,

                    NotificationType.PAYMENT_REQUESTED,
                    trip.getId()
            );
        } catch (Exception e) {
            // Log error
        }

        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public void approvePayment(Long paymentId, Long verifierId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("Payment not found"));

        // Chỉ người nhận tiền mới có quyền duyệt (nếu muốn làm thủ công)
        if (!payment.getToUser().getId().equals(verifierId)) {
            throw new BusinessException("Chuyển khoản này không gửi cho bạn, bạn không có quyền duyệt");
        }

        payment.setStatus(PaymentStatus.APPROVED);
        paymentRepository.save(payment);

        // 🚀 Bắn thông báo cho người chuyển tiền
        try {
            notificationService.sendNotification(
                    payment.getFromUser(),
                    "Thanh toán được phê duyệt: " + payment.getTrip().getName(),
                    payment.getToUser().getName() + " đã xác nhận nhận được số tiền " + CurrencyUtil.format(payment.getAmount()),

                    NotificationType.PAYMENT_APPROVED,
                    payment.getTrip().getId()
            );
        } catch (Exception e) {
            // Log error
        }
    }

    @Override
    @Transactional
    public void rejectPayment(Long paymentId, Long verifierId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("Payment not found"));

        if (!payment.getToUser().getId().equals(verifierId)) {
            throw new BusinessException("Bạn không có quyền từ chối chuyển khoản này");
        }

        payment.setStatus(PaymentStatus.REJECTED);
        paymentRepository.save(payment);

        // 🚀 Bắn thông báo cho người chuyển tiền
        try {
            notificationService.sendNotification(
                    payment.getFromUser(),
                    "Thanh toán bị từ chối: " + payment.getTrip().getName(),
                    payment.getToUser().getName() + " đã từ chối xác nhận số tiền " + CurrencyUtil.format(payment.getAmount()) + ". Vui lòng kiểm tra lại.",

                    NotificationType.SYSTEM,
                    payment.getTrip().getId()
            );
        } catch (Exception e) {
            // Log error
        }
    }

    @Override
    public List<PaymentResponse> getTripPayments(Long tripId) {
        return paymentRepository.findByTripId(tripId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createVirtualPayment(Long tripId, Long fromUserId, Long toUserId, BigDecimal amount, String reason) {

        if (amount.compareTo(new BigDecimal("9999999999999")) > 0) {
            throw new BusinessException("Số tiền thanh toán ảo quá lớn");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException("Trip not found"));
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new BusinessException("User not found"));
        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new BusinessException("User not found"));

        Payment payment = Payment.builder()
                .trip(trip)
                .fromUser(fromUser)
                .toUser(toUser)
                .amount(amount)
                .proofUrl(reason)
                .status(PaymentStatus.APPROVED)
                .build();
        paymentRepository.save(payment);
    }

    @Override
    public PageResponse<PaymentResponse> getTripPaymentsPaginated(Long tripId, PaymentStatus status, Long fromUserId, Long toUserId, Pageable pageable) {
        // Query dữ liệu với specification
        Page<Payment> paymentPage = paymentRepository.findAll(
                PaymentSpecification.filter(tripId, status, fromUserId, toUserId),
                pageable
        );

        // Map to response
        List<PaymentResponse> content = paymentPage.getContent().stream()
                .map(paymentMapper::toResponse)
                .toList();

        return PageResponse.<PaymentResponse>builder()
                .content(content)
                .pageNumber(paymentPage.getNumber())
                .pageSize(paymentPage.getSize())
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .last(paymentPage.isLast())
                .build();
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .tripId(payment.getTrip().getId())
                .fromUserId(payment.getFromUser().getId())
                .fromUserName(payment.getFromUser().getName())
                .toUserId(payment.getToUser().getId())
                .toUserName(payment.getToUser().getName())
                .amount(payment.getAmount())
                .proofUrl(payment.getProofUrl())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
