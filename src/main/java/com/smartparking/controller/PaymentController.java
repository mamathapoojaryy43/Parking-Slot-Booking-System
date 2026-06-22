package com.smartparking.controller;

import com.smartparking.model.Payment;
import com.smartparking.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> getPaymentByBooking(@PathVariable Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> {
                    Map<String, String> response = new HashMap<>();
                    response.put("error", "No transaction record exists for this booking!");
                    return ResponseEntity.badRequest().body(response);
                });
    }

    @PostMapping("/simulate")
    public ResponseEntity<?> simulatePayment(@RequestBody Map<String, Object> request) {
        // Mock successful merchant gateway authorization
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("transactionId", "TXN-" + System.currentTimeMillis() % 1000000);
        response.put("message", "Simulated online transaction authorized successfully.");
        return ResponseEntity.ok(response);
    }
}
