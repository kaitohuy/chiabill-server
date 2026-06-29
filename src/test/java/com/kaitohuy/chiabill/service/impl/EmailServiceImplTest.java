package com.kaitohuy.chiabill.service.impl;

import com.kaitohuy.chiabill.entity.User;
import com.kaitohuy.chiabill.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmailServiceImpl emailService;

    private User recipient;

    @BeforeEach
    void setUp() {
        recipient = new User();
        recipient.setId(1L);
        recipient.setEmail("test@example.com");
        recipient.setLanguage("vi");
    }

    @Test
    void sendInviteEmail_WhenReceiverIsVi_SendsViEmail() throws Exception {
        // GIVEN
        recipient.setLanguage("vi");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(recipient));
        
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // WHEN
        emailService.sendInviteEmail("test@example.com", "Đà Lạt 2026", "INV123");

        // THEN
        verify(mailSender).send(mimeMessage);
        
        // Subject check
        assertEquals("Mời bạn tham gia chuyến đi: Đà Lạt 2026", mimeMessage.getSubject());
        
        // Body check (HTML content)
        mimeMessage.saveChanges();
        String content = getTextFromPart(mimeMessage);
        System.out.println("DEBUG EMAIL CONTENT:\n" + content + "\n---");
        assertTrue(content.contains("Chào bạn,"));
        assertTrue(content.contains("Bạn vừa được mời tham gia chuyến đi"));
        assertTrue(content.contains("Tham Gia Chuyến Đi Trực Tiếp"));
        assertTrue(content.contains("Quét mã QR để vào nhóm"));
    }

    @Test
    void sendInviteEmail_WhenReceiverIsEn_SendsEnEmail() throws Exception {
        // GIVEN
        recipient.setLanguage("en");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(recipient));
        
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // WHEN
        emailService.sendInviteEmail("test@example.com", "Đà Lạt 2026", "INV123");

        // THEN
        verify(mailSender).send(mimeMessage);
        
        // Subject check
        assertEquals("Invitation to join trip: Đà Lạt 2026", mimeMessage.getSubject());
        
        // Body check (HTML content)
        mimeMessage.saveChanges();
        String content = getTextFromPart(mimeMessage);
        assertTrue(content.contains("Hello,"));
        assertTrue(content.contains("You have been invited to join the trip"));
        assertTrue(content.contains("Join Trip Directly"));
        assertTrue(content.contains("Scan QR code to join group"));
    }

    private String getTextFromPart(jakarta.mail.Part part) throws Exception {
        Object content = part.getContent();
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof java.io.InputStream) {
            java.io.InputStream is = (java.io.InputStream) content;
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } else if (part.isMimeType("multipart/*") && content instanceof jakarta.mail.internet.MimeMultipart) {
            jakarta.mail.internet.MimeMultipart multipart = (jakarta.mail.internet.MimeMultipart) content;
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                result.append(getTextFromPart(multipart.getBodyPart(i)));
            }
            return result.toString();
        }
        return "";
    }
}
