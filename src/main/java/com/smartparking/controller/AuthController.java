package com.smartparking.controller;

import com.smartparking.model.User;
import com.smartparking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            User registered = userService.registerUser(user);
            return ResponseEntity.ok(registered);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/login-status")
    public ResponseEntity<?> getLoginStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.ok(null);
        }

        Optional<User> userOpt = userService.findByUsername(auth.getName());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> details = new HashMap<>();
            details.put("id", user.getId());
            details.put("username", user.getUsername());
            details.put("email", user.getEmail());
            details.put("fullName", user.getFullName());
            details.put("phone", user.getPhone());
            details.put("isAdmin", auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
            return ResponseEntity.ok(details);
        }

        return ResponseEntity.ok(null);
    }
}
