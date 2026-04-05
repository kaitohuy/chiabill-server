package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.service.interfaces.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    @Async
    public void sendInviteEmail(String toEmail, String tripName, String inviteCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Mời bạn tham gia chuyến đi: " + tripName);

            String content = "Chào bạn,\n\n" +
                    "Bạn vừa được mời tham gia chuyến đi '" + tripName + "' trên ứng dụng ChiaBill.\n" +
                    "Vì bạn đã tắt quyền tự động thêm vào nhóm, vui lòng sử dụng mã mời bên dưới để tham gia:\n\n" +
                    "Mã mời: " + inviteCode + "\n\n" +
                    "Trân trọng,\n" +
                    "Đội ngũ ChiaBill";

            helper.setText(content);
            mailSender.send(message);
            log.info("Invite email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send invite email to {}: {}", toEmail, e.getMessage());
        }
    }
}
