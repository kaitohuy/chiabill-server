package com.kaitohuy.chiabill.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // Auth & User errors
    USER_NOT_FOUND("USER_NOT_FOUND", "Không tìm thấy người dùng"),
    EMAIL_OR_PHONE_REQUIRED("EMAIL_OR_PHONE_REQUIRED", "Email hoặc số điện thoại không được để trống"),
    ACCESS_DENIED("ACCESS_DENIED", "Không có quyền truy cập"),
    ACCESS_DENIED_NOT_MEMBER("ACCESS_DENIED_NOT_MEMBER", "Không có quyền truy cập: bạn không phải thành viên của chuyến đi"),

    // Trip & Members
    TRIP_NOT_FOUND("TRIP_NOT_FOUND", "Không tìm thấy chuyến đi"),
    TRIP_HAS_BEEN_DELETED("TRIP_HAS_BEEN_DELETED", "Chuyến đi đã bị xóa"),
    ACTOR_NOT_FOUND("ACTOR_NOT_FOUND", "Không tìm thấy người thực hiện"),
    OWNER_NOT_FOUND("OWNER_NOT_FOUND", "Không tìm thấy chủ nhóm"),
    CREDITOR_NOT_FOUND("CREDITOR_NOT_FOUND", "Không tìm thấy chủ nợ"),
    USER_ALREADY_IN_TRIP("USER_ALREADY_IN_TRIP", "Người dùng đã là thành viên của chuyến đi"),
    USER_NOT_IN_TRIP("USER_NOT_IN_TRIP", "Người dùng không thuộc chuyến đi này"),
    USER_NOT_ACTIVE_IN_TRIP("USER_NOT_ACTIVE_IN_TRIP", "Người dùng không hoạt động trong chuyến đi này"),

    // Payments & Expenses
    PAYMENT_LIMIT_EXCEEDED("PAYMENT_LIMIT_EXCEEDED", "Số tiền thanh toán quá lớn, vui lòng kiểm tra lại"),
    VALIDATION_ERROR("VALIDATION_ERROR", "Yêu cầu không hợp lệ"),

    // Specific business constraints
    EMAIL_NOT_UPDATED("EMAIL_NOT_UPDATED", "Bạn phải cập nhật Email trước khi tắt quyền tự động thêm vào nhóm mẫu."),
    MEMBER_AUTO_ADD_NOT_ALLOWED("MEMBER_AUTO_ADD_NOT_ALLOWED", "Người dùng này không cho phép thêm tự động. Hệ thống đã gửi email lời mời."),
    TRIP_NOT_IN_TRASH("TRIP_NOT_IN_TRASH", "Chuyến đi chưa được chuyển vào thùng rác. Không thể xóa vĩnh viễn."),
    OWNER_CANNOT_LEAVE("OWNER_CANNOT_LEAVE", "Chủ nhà không thể rời nhóm khi vẫn còn thành viên khác. Hãy chuyển quyền (Transfer Owner) cho người khác trước."),
    DEBT_UNSETTLED("DEBT_UNSETTLED", "Bạn vẫn còn khoản nợ chưa thanh toán. Vui lòng tất toán trước khi tự rời nhóm. Nếu bị đuổi, nợ sẽ xử lý theo yêu cầu của Chủ phòng."),
    CANNOT_KICK_SELF("CANNOT_KICK_SELF", "Bạn không thể tự đuổi chính mình"),
    ONLY_OWNER_ALLOWED("ONLY_OWNER_ALLOWED", "Chỉ chủ nhóm mới được phép thực hiện thao tác này"),
    ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản với thông tin đã cung cấp."),
    SEARCH_IDENTIFIER_REQUIRED("SEARCH_IDENTIFIER_REQUIRED", "Vui lòng cung cấp Email hoặc Số điện thoại để tìm kiếm thành viên."),
    RECIPIENT_NOT_IN_TRIP("RECIPIENT_NOT_IN_TRIP", "Người nhận quyền không thuộc chuyến đi này"),
    CANNOT_TRANSFER_TO_INACTIVE("CANNOT_TRANSFER_TO_INACTIVE", "Không thể chuyển quyền cho người đã rời nhóm"),
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "Thành viên không tồn tại"),
    USER_LEFT_TRIP("USER_LEFT_TRIP", "Bạn đã rời khỏi chuyến đi này và không thể xem chi tiết."),

    // Fallback/System errors
    SYSTEM_ERROR("SYSTEM_ERROR", "Đã có lỗi xảy ra trên hệ thống"),
    UNKNOWN_ERROR("UNKNOWN_ERROR", "Lỗi không xác định");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
