package com.smartparking.controller;

import com.smartparking.model.Notification;
import com.smartparking.model.User;
import com.smartparking.service.NotificationService;
import com.smartparking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @Autowired
    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found!"));
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications() {
        User user = getCurrentUser();
        return ResponseEntity.ok(notificationService.getNotificationsForUser(user.getId()));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> readAll() {
        User user = getCurrentUser();
        notificationService.markAllAsRead(user.getId());
        Map<String, String> response = new HashMap<>();
        response.put("message", "All notifications marked as read.");
        return ResponseEntity.ok(response);
    }
}
