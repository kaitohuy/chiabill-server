package com.kaitohuy.chiabill.service.interfaces;

public interface EmailService {
    void sendInviteEmail(String toEmail, String tripName, String inviteCode);
}
