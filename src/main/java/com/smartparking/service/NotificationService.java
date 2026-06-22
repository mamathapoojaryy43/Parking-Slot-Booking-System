package com.smartparking.service;

import com.smartparking.model.Notification;
import com.smartparking.model.User;
import com.smartparking.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification sendNotification(User user, String message) {
        Notification notification = new Notification(user, message);
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdAndReadStatusOrderByCreatedAtDesc(userId, false);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadStatusOrderByCreatedAtDesc(userId, false);
        for (Notification n : unread) {
            n.setReadStatus(true);
        }
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setReadStatus(true);
            notificationRepository.save(n);
        });
    }
}
