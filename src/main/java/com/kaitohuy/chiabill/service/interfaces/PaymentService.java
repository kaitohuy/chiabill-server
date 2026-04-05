package com.kaitohuy.chiabill.service.interfaces;

import com.kaitohuy.chiabill.dto.response.PageResponse;
import com.kaitohuy.chiabill.dto.response.PaymentResponse;
import com.kaitohuy.chiabill.entity.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(Long tripId, Long fromUserId, Long toUserId, BigDecimal amount, MultipartFile proof);

    void approvePayment(Long paymentId, Long verifierId);

    void rejectPayment(Long paymentId, Long verifierId);

    List<PaymentResponse> getTripPayments(Long tripId);

    PageResponse<PaymentResponse> getTripPaymentsPaginated(Long tripId, PaymentStatus status, Long fromUserId, Long toUserId, Pageable pageable);

    void createVirtualPayment(Long tripId, Long fromUserId, Long toUserId, BigDecimal amount, String reason);
}
