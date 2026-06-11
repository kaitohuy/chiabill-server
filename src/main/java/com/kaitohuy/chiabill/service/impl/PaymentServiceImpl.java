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
    private final com.kaitohuy.chiabill.service.interfaces.TripHistoryService tripHistoryService;
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

        // Upload minh chứng lên Cloudinary (nếu có)
        String proofUrl = (proof != null && !proof.isEmpty()) ? cloudinaryService.uploadImage(proof) : null;

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
        
        tripHistoryService.logPaymentRequest(fromUser, payment);

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
        
        tripHistoryService.logPaymentApprove(payment.getToUser(), payment);
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
        
        tripHistoryService.logPaymentReject(payment.getToUser(), payment);
    }

    @Override
    public List<PaymentResponse> getTripPayments(Long tripId) {
        return paymentRepository.findByTripIdAndIsDeletedFalse(tripId).stream()
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
    @Transactional
    public PaymentResponse createBatchPayOnBehalf(Long tripId, Long payerId, com.kaitohuy.chiabill.dto.request.BatchPayOnBehalfRequest request, MultipartFile proof) {
        if (request.getTotalAmount().compareTo(new BigDecimal("9999999999999")) > 0) {
            throw new BusinessException("Số tiền thanh toán quá lớn");
        }
        if (request.getOnBehalfOfUserIds() == null || request.getOnBehalfOfUserIds().isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất một người cần thanh toán hộ");
        }
        if (request.getOnBehalfOfAmounts() == null || request.getOnBehalfOfAmounts().size() != request.getOnBehalfOfUserIds().size()) {
            throw new BusinessException("Danh sách số tiền không hợp lệ");
        }
        if (!tripMemberRepository.existsByTripIdAndUserId(tripId, payerId)) {
            throw new BusinessException("Bạn không thuộc chuyến đi này");
        }
        if (!tripMemberRepository.existsByTripIdAndUserId(tripId, request.getToUserId())) {
            throw new BusinessException("Người nhận không thuộc chuyến đi này");
        }

        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new BusinessException("Trip not found"));
        User payer = userRepository.findById(payerId).orElseThrow(() -> new BusinessException("Người dùng không tồn tại"));
        User toUser = userRepository.findById(request.getToUserId()).orElseThrow(() -> new BusinessException("Người nhận không tồn tại"));

        List<User> onBehalfOfUsers = request.getOnBehalfOfUserIds().stream()
                .map(id -> userRepository.findById(id).orElseThrow(() -> new BusinessException("Người dùng id=" + id + " không tồn tại")))
                .collect(java.util.stream.Collectors.toList());

        String proofUrl = (proof != null && !proof.isEmpty()) ? cloudinaryService.uploadImage(proof) : null;
        PaymentStatus initialStatus = Boolean.TRUE.equals(toUser.getAllowAutoApprovePayment())
                ? PaymentStatus.APPROVED : PaymentStatus.PENDING;

        List<Payment> savedPayments = new java.util.ArrayList<>();
        for (int i = 0; i < onBehalfOfUsers.size(); i++) {
            User behalfUser = onBehalfOfUsers.get(i);
            BigDecimal amount = request.getOnBehalfOfAmounts().get(i);

            Payment payment = Payment.builder()
                    .trip(trip)
                    .fromUser(behalfUser) // Người nợ đứng tên gửi để trừ nợ
                    .toUser(toUser)
                    .onBehalfOfUser(payer) // Ghi nhận A đã trả hộ
                    .amount(amount)
                    .proofUrl(proofUrl)
                    .status(initialStatus)
                    .build();
            paymentRepository.save(payment);
            savedPayments.add(payment);
        }

        String names = onBehalfOfUsers.stream().map(User::getName).collect(java.util.stream.Collectors.joining(", "));
        try {
            notificationService.sendNotification(toUser,
                    "Thanh toán hộ: " + trip.getName(),
                    payer.getName() + " đã thanh toán hộ cho [" + names + "] tổng cộng " + CurrencyUtil.format(request.getTotalAmount()),
                    NotificationType.PAYMENT_REQUESTED, trip.getId());
        } catch (Exception ignored) {}

        Payment dummyForLog = Payment.builder().trip(trip).toUser(toUser).amount(request.getTotalAmount()).build();
        tripHistoryService.logPayOnBehalf(payer, dummyForLog, onBehalfOfUsers);

        return PaymentResponse.builder()
                .id(savedPayments.get(0).getId()).tripId(trip.getId())
                .fromUserId(payer.getId()).fromUserName(payer.getName())
                .toUserId(toUser.getId()).toUserName(toUser.getName())
                .amount(request.getTotalAmount()).proofUrl(proofUrl).status(initialStatus)
                .createdAt(savedPayments.get(0).getCreatedAt())
                .onBehalfOfUserIds(request.getOnBehalfOfUserIds())
                .onBehalfOfUserNames(onBehalfOfUsers.stream().map(User::getName).collect(java.util.stream.Collectors.toList()))
                .build();
    }


    @Override
    public PageResponse<PaymentResponse> getTripPaymentsPaginated(Long tripId, PaymentStatus status, Long fromUserId, Long toUserId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Pageable pageable) {
        // Query dữ liệu với specification
        Page<Payment> paymentPage = paymentRepository.findAll(
                PaymentSpecification.filter(tripId, status, fromUserId, toUserId, startDate, endDate),
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
