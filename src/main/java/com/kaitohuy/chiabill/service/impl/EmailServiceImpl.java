package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.service.interfaces.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${base_url}")
    private String baseUrl;

    @Override
    @Async
    public void sendInviteEmail(String toEmail, String tripName, String inviteCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Mời bạn tham gia chuyến đi: " + tripName);

            // Xử lý tạo link mời tham gia chuyến đi
            String activeBaseUrl = (baseUrl == null || baseUrl.isEmpty()) ? "https://chiabill-server.onrender.com" : baseUrl;
            if (activeBaseUrl.endsWith("/")) {
                activeBaseUrl = activeBaseUrl.substring(0, activeBaseUrl.length() - 1);
            }
            String joinUrl = activeBaseUrl + "/join/" + inviteCode;
            String encodedJoinUrl = URLEncoder.encode(joinUrl, StandardCharsets.UTF_8);

            // Giao diện email HTML
            String htmlContent = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>Mời tham gia chuyến đi</title>\n" +
                    "</head>\n" +
                    "<body style=\"margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f6f9fc; color: #333333;\">\n" +
                    "    <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"table-layout: fixed; background-color: #f6f9fc;\">\n" +
                    "        <tr>\n" +
                    "            <td align=\"center\" style=\"padding: 40px 0;\">\n" +
                    "                <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\" style=\"background-color: #ffffff; border-radius: 16px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); overflow: hidden;\">\n" +
                    "                    <!-- Header -->\n" +
                    "                    <tr>\n" +
                    "                        <td align=\"center\" style=\"background: linear-gradient(135deg, #FF4B2B, #FF416C); padding: 40px 20px;\">\n" +
                    "                            <h1 style=\"color: #ffffff; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: 0.5px;\">ChiaBill</h1>\n" +
                    "                            <p style=\"color: rgba(255, 255, 255, 0.9); margin: 8px 0 0 0; font-size: 14px;\">Chia tiền thông minh - Chuyến đi trọn vẹn</p>\n" +
                    "                        </td>\n" +
                    "                    </tr>\n" +
                    "                    <!-- Content -->\n" +
                    "                    <tr>\n" +
                    "                        <td style=\"padding: 40px 30px;\">\n" +
                    "                            <p style=\"margin: 0 0 20px 0; font-size: 16px; line-height: 1.6; color: #4A4A4A;\">Chào bạn,</p>\n" +
                    "                            <p style=\"margin: 0 0 20px 0; font-size: 16px; line-height: 1.6; color: #4A4A4A;\">\n" +
                    "                                Bạn vừa được mời tham gia chuyến đi <strong style=\"color: #FF4B2B;\">" + tripName + "</strong> trên ứng dụng ChiaBill.\n" +
                    "                            </p>\n" +
                    "                            <p style=\"margin: 0 0 30px 0; font-size: 14px; line-height: 1.6; color: #777777;\">\n" +
                    "                                Vì bạn đã tắt quyền tự động thêm vào nhóm, vui lòng sử dụng một trong các cách dưới đây để tham gia chuyến đi:\n" +
                    "                            </p>\n" +
                    "\n" +
                    "                            <!-- Cách 1: Click tham gia trực tiếp -->\n" +
                    "                            <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"margin-bottom: 35px;\">\n" +
                    "                                <tr>\n" +
                    "                                    <td align=\"center\">\n" +
                    "                                        <a href=\"" + joinUrl + "\" target=\"_blank\" style=\"background-color: #FF4B2B; color: #ffffff; text-decoration: none; padding: 14px 30px; font-size: 16px; font-weight: bold; border-radius: 12px; display: inline-block; box-shadow: 0 4px 10px rgba(255, 75, 43, 0.3);\">\n" +
                    "                                            Tham Gia Chuyến Đi Trực Tiếp\n" +
                    "                                        </a>\n" +
                    "                                    </td>\n" +
                    "                                </tr>\n" +
                    "                            </table>\n" +
                    "\n" +
                    "                            <hr style=\"border: 0; border-top: 1px solid #eeeeee; margin: 30px 0;\" />\n" +
                    "\n" +
                    "                            <!-- Cách 2 & 3: Mã QR & Nhập Code -->\n" +
                    "                            <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                    "                                <tr>\n" +
                    "                                    <!-- Mã QR -->\n" +
                    "                                    <td align=\"center\" width=\"50%\" style=\"padding-right: 15px; vertical-align: top;\">\n" +
                    "                                        <p style=\"margin: 0 0 12px 0; font-size: 13px; font-weight: bold; color: #666666; text-transform: uppercase; letter-spacing: 0.5px;\">Quét mã QR để vào nhóm</p>\n" +
                    "                                        <div style=\"padding: 10px; border: 1px solid #e1e8ed; border-radius: 12px; display: inline-block; background-color: #ffffff;\">\n" +
                    "                                            <img src=\"https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + encodedJoinUrl + "\" alt=\"Mã QR tham gia\" width=\"150\" height=\"150\" style=\"display: block; border: 0;\" />\n" +
                    "                                        </div>\n" +
                    "                                    </td>\n" +
                    "                                    <!-- Nhập Code -->\n" +
                    "                                    <td align=\"center\" width=\"50%\" style=\"padding-left: 15px; vertical-align: top;\">\n" +
                    "                                        <p style=\"margin: 0 0 12px 0; font-size: 13px; font-weight: bold; color: #666666; text-transform: uppercase; letter-spacing: 0.5px;\">Nhập mã mời trong ứng dụng</p>\n" +
                    "                                        <div style=\"background-color: #f8f9fa; border: 1px dashed #cccccc; border-radius: 12px; padding: 20px 15px; display: inline-block; width: 80%;\">\n" +
                    "                                            <span style=\"font-family: monospace; font-size: 22px; font-weight: bold; color: #333333; letter-spacing: 2px;\">" + inviteCode + "</span>\n" +
                    "                                        </div>\n" +
                    "                                    </td>\n" +
                    "                                </tr>\n" +
                    "                            </table>\n" +
                    "\n" +
                    "                            <hr style=\"border: 0; border-top: 1px solid #eeeeee; margin: 30px 0;\" />\n" +
                    "\n" +
                    "                            <p style=\"margin: 0; font-size: 15px; line-height: 1.6; color: #4A4A4A;\">\n" +
                    "                                Trân trọng,<br/>\n" +
                    "                                <strong style=\"color: #FF4B2B;\">Đội ngũ ChiaBill</strong>\n" +
                    "                            </p>\n" +
                    "                        </td>\n" +
                    "                    </tr>\n" +
                    "                    <!-- Footer -->\n" +
                    "                    <tr>\n" +
                    "                        <td align=\"center\" style=\"background-color: #f9f9f9; padding: 20px; font-size: 12px; color: #999999; border-top: 1px solid #eeeeee;\">\n" +
                    "                            Email này được gửi tự động từ hệ thống ChiaBill.<br/>\n" +
                    "                            Vui lòng không phản hồi trực tiếp email này.\n" +
                    "                        </td>\n" +
                    "                    </tr>\n" +
                    "                </table>\n" +
                    "            </td>\n" +
                    "        </tr>\n" +
                    "    </table>\n" +
                    "</body>\n" +
                    "</html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Invite email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send invite email to {}: {}", toEmail, e.getMessage());
        }
    }
}
