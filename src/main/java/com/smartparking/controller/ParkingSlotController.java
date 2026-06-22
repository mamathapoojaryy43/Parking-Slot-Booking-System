package com.smartparking.controller;

import com.smartparking.model.*;
import com.smartparking.service.ParkingSlotService;
import com.smartparking.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/slots")
public class ParkingSlotController {

    private final ParkingSlotService parkingSlotService;
    private final VehicleService vehicleService;

    @Autowired
    public ParkingSlotController(ParkingSlotService parkingSlotService, VehicleService vehicleService) {
        this.parkingSlotService = parkingSlotService;
        this.vehicleService = vehicleService;
    }

    @GetMapping("/floors")
    public ResponseEntity<List<Floor>> getFloors() {
        return ResponseEntity.ok(parkingSlotService.getAllFloors());
    }

    @GetMapping("/floors/{floorId}/slots")
    public ResponseEntity<List<ParkingSlot>> getFloorSlots(@PathVariable Long floorId) {
        return ResponseEntity.ok(parkingSlotService.getSlotsByFloor(floorId));
    }

    @GetMapping("/recommend")
    public ResponseEntity<?> recommendSlot(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false, defaultValue = "false") boolean requiresDisabled) {

        VehicleType type = VehicleType.FOUR_WHEELER;
        if (vehicleId != null) {
            Optional<Vehicle> vehicleOpt = vehicleService.getVehicleById(vehicleId);
            if (vehicleOpt.isPresent()) {
                type = vehicleOpt.get().getType();
            }
        }

        Optional<ParkingSlot> recommended = parkingSlotService.recommendBestSlot(type, requiresDisabled);

        if (recommended.isPresent()) {
            return ResponseEntity.ok(recommended.get());
        }

        Map<String, String> response = new HashMap<>();
        response.put("error", "No vacant slots matching search parameters could be found on any floor!");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("available", parkingSlotService.getCountByStatus(SlotStatus.AVAILABLE));
        stats.put("occupied", parkingSlotService.getCountByStatus(SlotStatus.OCCUPIED));
        stats.put("reserved", parkingSlotService.getCountByStatus(SlotStatus.RESERVED));
        return ResponseEntity.ok(stats);
    }
}
