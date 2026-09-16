package com.sharemile.service;

import com.sharemile.model.Notification;
import com.sharemile.model.User;
import com.sharemile.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public Notification sendNotification(User recipient, String title, String message, String type) {
        Notification notification = new Notification(recipient, title, message, type);
        Notification saved = notificationRepository.save(notification);

        // Push real-time alert via Spring WebSocket STOMP endpoint
        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + recipient.getId(), saved);
        } catch (Exception ignored) {
            // If websocket client is not connected, notification remains safely in DB
        }

        return saved;
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setReadStatus(true);
            notificationRepository.save(n);
        });
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadStatusFalse(userId);
    }
}
