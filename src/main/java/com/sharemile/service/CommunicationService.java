package com.sharemile.service;

import com.sharemile.model.Notification;
import com.sharemile.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CommunicationService {

    private static final Logger log = LoggerFactory.getLogger(CommunicationService.class);
    private final NotificationService notificationService;

    public CommunicationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Dispatches multi-channel notification: WebSocket STOMP, Email Alert, and SMS (if phone provided).
     */
    public void notifyMultiChannel(User user, String title, String message, String type) {
        // 1. In-app and STOMP WebSocket notification
        Notification n = notificationService.sendNotification(user, title, message, type);

        // 2. Email Notification dispatch
        sendEmail(user.getEmail(), user.getFullName(), title, message);

        // 3. SMS Notification dispatch (if phone number is available)
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            sendSms(user.getPhone().trim(), user.getFullName(), title + ": " + message);
        }
    }

    public void sendEmail(String toEmail, String recipientName, String subject, String body) {
        if (toEmail == null || toEmail.trim().isEmpty()) return;
        log.info("📧 [EMAIL DISPATCHED] To: {} <{}> | Subject: [{}] | Body: {}", recipientName, toEmail, subject, body);
    }

    public void sendSms(String phoneNumber, String recipientName, String message) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) return;
        log.info("📱 [SMS DISPATCHED] To: {} ({}) | Body: {}", recipientName, phoneNumber, message);
    }
}
