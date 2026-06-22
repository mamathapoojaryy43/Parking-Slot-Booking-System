package com.smartparking.controller;

import com.smartparking.model.User;
import com.smartparking.model.Vehicle;
import com.smartparking.service.UserService;
import com.smartparking.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final UserService userService;

    @Autowired
    public VehicleController(VehicleService vehicleService, UserService userService) {
        this.vehicleService = vehicleService;
        this.userService = userService;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userService.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Current user not found!"));
    }

    @GetMapping
    public ResponseEntity<List<Vehicle>> getMyVehicles() {
        User user = getCurrentUser();
        return ResponseEntity.ok(vehicleService.getVehiclesByOwner(user));
    }

    @PostMapping
    public ResponseEntity<?> registerVehicle(@RequestBody Vehicle vehicle) {
        try {
            User user = getCurrentUser();
            vehicle.setOwner(user);
            Vehicle saved = vehicleService.registerVehicle(vehicle);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        try {
            vehicleService.deleteVehicle(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Vehicle unregistered successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
