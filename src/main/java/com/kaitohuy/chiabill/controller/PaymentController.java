package com.kaitohuy.chiabill.controller;

import com.kaitohuy.chiabill.dto.response.ApiResponse;
import com.kaitohuy.chiabill.dto.response.PaymentResponse;
import com.kaitohuy.chiabill.security.UserPrincipal;
import com.kaitohuy.chiabill.service.interfaces.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping(value = "/trips/{tripId}/payments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PaymentResponse> createPayment(
            @PathVariable Long tripId,
            @RequestParam("toUserId") Long toUserId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("proof") MultipartFile proof,
            Authentication authentication) {

        Long fromUserId = ((UserPrincipal) authentication.getPrincipal()).getUserId();

        return ApiResponse.<PaymentResponse>builder()
                .success(true)
                .data(paymentService.createPayment(tripId, fromUserId, toUserId, amount, proof))
                .build();
    }

    @GetMapping("/trip/{tripId}")
    public ApiResponse<List<PaymentResponse>> getTripPayments(@PathVariable Long tripId) {
        return ApiResponse.<List<PaymentResponse>>builder()
                .success(true)
                .data(paymentService.getTripPayments(tripId))
                .build();
    }

    @GetMapping("/trip/{tripId}/search")
    public ApiResponse<com.kaitohuy.chiabill.dto.response.PageResponse<PaymentResponse>> getTripPaymentsPaginated(
            @PathVariable Long tripId,
            @RequestParam(required = false) com.kaitohuy.chiabill.entity.PaymentStatus status,
            @RequestParam(required = false) Long fromUserId,
            @RequestParam(required = false) Long toUserId,
            org.springframework.data.domain.Pageable pageable) {

        return ApiResponse.<com.kaitohuy.chiabill.dto.response.PageResponse<PaymentResponse>>builder()
                .success(true)
                .data(paymentService.getTripPaymentsPaginated(tripId, status, fromUserId, toUserId, pageable))
                .build();
    }

    @PutMapping("/payments/{paymentId}/approve")
    public ApiResponse<String> approvePayment(
            @PathVariable Long paymentId,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        paymentService.approvePayment(paymentId, userId);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Đã duyệt thanh toán thành công")
                .build();
    }

    @PutMapping("/payments/{paymentId}/reject")
    public ApiResponse<String> rejectPayment(
            @PathVariable Long paymentId,
            Authentication authentication) {

        Long userId = ((UserPrincipal) authentication.getPrincipal()).getUserId();
        paymentService.rejectPayment(paymentId, userId);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Đã từ chối thanh toán")
                .build();
    }
}
