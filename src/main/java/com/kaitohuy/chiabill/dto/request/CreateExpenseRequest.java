package com.kaitohuy.chiabill.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateExpenseRequest {

    @NotNull(message = "ID chuyến đi là bắt buộc")
    private Long tripId;

    @NotNull(message = "Người trả tiền là bắt buộc")
    private Long payerId;

    @NotNull(message = "Tổng số tiền là bắt buộc")
    @Positive(message = "Tổng số tiền phải lớn hơn 0")
    private BigDecimal totalAmount;

    @NotBlank(message = "Mô tả không được để trống")
    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    private String description;

    @NotNull(message = "Danh mục chi phí là bắt buộc")
    private Long categoryId;

    @NotNull(message = "Ngày chi tiêu là bắt buộc")
    private LocalDateTime expenseDate;

    @Size(max = 500, message = "URL hóa đơn không được vượt quá 500 ký tự")
    private String receiptUrl;

    @Size(max = 10, message = "Đơn vị tiền tệ không được vượt quá 10 ký tự")
    private String currency;

    private BigDecimal exchangeRate;

    private Boolean isFromFund;

    @Size(max = 50, message = "Client UUID không được vượt quá 50 ký tự")
    private String clientUuid;

    @Size(max = 50, message = "Loại chia tiền không được vượt quá 50 ký tự")
    private String splitType;

    @NotEmpty(message = "Danh sách chia tiền không được để trống")
    @Valid
    private List<SplitRequest> splits;
}