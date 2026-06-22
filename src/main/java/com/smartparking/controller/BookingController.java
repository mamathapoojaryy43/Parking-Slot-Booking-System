package com.smartparking.controller;

import com.smartparking.model.*;
import com.smartparking.service.BookingService;
import com.smartparking.service.ParkingSlotService;
import com.smartparking.service.UserService;
import com.smartparking.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;
    private final ParkingSlotService parkingSlotService;
    private final VehicleService vehicleService;

    @Autowired
    public BookingController(BookingService bookingService, UserService userService,
                             ParkingSlotService parkingSlotService, VehicleService vehicleService) {
        this.bookingService = bookingService;
        this.userService = userService;
        this.parkingSlotService = parkingSlotService;
        this.vehicleService = vehicleService;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found!"));
    }

    @GetMapping("/history")
    public ResponseEntity<List<Booking>> getMyHistory() {
        User user = getCurrentUser();
        return ResponseEntity.ok(bookingService.getBookingsByUser(user.getId()));
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserve(@RequestBody Map<String, Object> request) {
        try {
            User user = getCurrentUser();
            Long slotId = Long.valueOf(request.get("slotId").toString());
            Long vehicleId = Long.valueOf(request.get("vehicleId").toString());
            String startTimeStr = request.get("startTime").toString();
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr);

            ParkingSlot slot = parkingSlotService.getSlotById(slotId)
                    .orElseThrow(() -> new RuntimeException("Slot not found!"));
            Vehicle vehicle = vehicleService.getVehicleById(vehicleId)
                    .orElseThrow(() -> new RuntimeException("Vehicle not found!"));

            Booking booking = bookingService.reserveSlot(user, slot, vehicle, startTime);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/entry")
    public ResponseEntity<?> entryCheckIn(@RequestBody Map<String, String> request) {
        try {
            String bookingNumber = request.get("bookingNumber");
            Booking booking = bookingService.checkIn(bookingNumber);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/exit")
    public ResponseEntity<?> exitCheckOut(@RequestBody Map<String, String> request) {
        try {
            String bookingNumber = request.get("bookingNumber");
            String paymentMethod = request.getOrDefault("paymentMethod", "CARD");
            Booking booking = bookingService.checkOut(bookingNumber, paymentMethod);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
