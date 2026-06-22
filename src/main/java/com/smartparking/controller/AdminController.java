package com.smartparking.controller;

import com.smartparking.model.*;
import com.smartparking.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final ParkingSlotService parkingSlotService;
    private final BookingService bookingService;
    private final PricingService pricingService;
    private final ReportService reportService;

    @Autowired
    public AdminController(UserService userService, ParkingSlotService parkingSlotService,
                           BookingService bookingService, PricingService pricingService,
                           ReportService reportService) {
        this.userService = userService;
        this.parkingSlotService = parkingSlotService;
        this.bookingService = bookingService;
        this.pricingService = pricingService;
        this.reportService = reportService;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long activeReservations = bookingService.getAllBookings().stream()
                .filter(b -> b.getStatus() == BookingStatus.RESERVED)
                .count();

        long occupiedSpots = parkingSlotService.getCountByStatus(SlotStatus.OCCUPIED);

        LocalDate today = LocalDate.now();
        List<ParkingReport> reports = reportService.getReportsForLastDays(7);
        
        // Ensure today's summary is generated for live data feed
        ParkingReport todayReport = reportService.generateDailySummary(today);
        
        double totalRevenue = reports.stream().mapToDouble(ParkingReport::getTotalRevenue).sum() + todayReport.getTotalRevenue();
        int todayBookings = todayReport.getTotalBookings();
        double averageOccupancy = pricingService.getOccupancyRate() * 100.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeReservations", activeReservations);
        stats.put("occupiedSpots", occupiedSpots);
        stats.put("todayBookings", todayBookings);
        stats.put("totalRevenue", totalRevenue);
        stats.put("averageOccupancy", Math.round(averageOccupancy * 10.0) / 10.0);
        stats.put("co2SavedTotal", reportService.calculateTotalCO2Saved());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/reports")
    public ResponseEntity<List<ParkingReport>> getReports() {
        // Trigger summary generate for today to ensure it's in the list
        reportService.generateDailySummary(LocalDate.now());
        return ResponseEntity.ok(reportService.getReportsForLastDays(7));
    }

    @GetMapping("/prediction")
    public ResponseEntity<Map<Integer, Double>> getPrediction() {
        return ResponseEntity.ok(reportService.predictHourlyOccupancy(LocalDate.now()));
    }

    @GetMapping("/pricing")
    public ResponseEntity<?> getPricingConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", pricingService.isDynamicPricingEnabled());
        config.put("peakThreshold", pricingService.getPeakOccupancyThreshold());
        config.put("peakMultiplier", pricingService.getPeakPriceMultiplier());
        config.put("mediumThreshold", pricingService.getMediumOccupancyThreshold());
        config.put("mediumMultiplier", pricingService.getMediumPriceMultiplier());
        config.put("discountThreshold", pricingService.getDiscountOccupancyThreshold());
        config.put("discountMultiplier", pricingService.getDiscountPriceMultiplier());
        return ResponseEntity.ok(config);
    }

    @PostMapping("/pricing")
    public ResponseEntity<?> updatePricingConfig(@RequestBody Map<String, Object> config) {
        try {
            if (config.containsKey("enabled")) {
                pricingService.setDynamicPricingEnabled((Boolean) config.get("enabled"));
            }
            if (config.containsKey("peakThreshold")) {
                pricingService.setPeakOccupancyThreshold(Double.parseDouble(config.get("peakThreshold").toString()));
            }
            if (config.containsKey("peakMultiplier")) {
                pricingService.setPeakPriceMultiplier(Double.parseDouble(config.get("peakMultiplier").toString()));
            }
            if (config.containsKey("mediumThreshold")) {
                pricingService.setMediumOccupancyThreshold(Double.parseDouble(config.get("mediumThreshold").toString()));
            }
            if (config.containsKey("mediumMultiplier")) {
                pricingService.setMediumPriceMultiplier(Double.parseDouble(config.get("mediumMultiplier").toString()));
            }
            if (config.containsKey("discountThreshold")) {
                pricingService.setDiscountOccupancyThreshold(Double.parseDouble(config.get("discountThreshold").toString()));
            }
            if (config.containsKey("discountMultiplier")) {
                pricingService.setDiscountPriceMultiplier(Double.parseDouble(config.get("discountMultiplier").toString()));
            }

            Map<String, String> response = new HashMap<>();
            response.put("message", "Dynamic Pricing Engine parameters updated successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping("/floors")
    public ResponseEntity<?> createFloor(@RequestBody Floor floor) {
        try {
            Floor saved = parkingSlotService.saveFloor(floor);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/slots")
    public ResponseEntity<?> createSlot(@RequestBody Map<String, Object> request) {
        try {
            String slotNumber = (String) request.get("slotNumber");
            SlotType type = SlotType.valueOf((String) request.get("type"));
            Long floorId = Long.valueOf(request.get("floorId").toString());

            Floor floor = parkingSlotService.getFloorById(floorId)
                    .orElseThrow(() -> new RuntimeException("Floor not found!"));

            ParkingSlot slot = new ParkingSlot(slotNumber, type, SlotStatus.AVAILABLE, floor);
            ParkingSlot saved = parkingSlotService.saveSlot(slot);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/export/revenue")
    public ResponseEntity<String> exportRevenueCSV() {
        String csvData = reportService.generateRevenueCSV();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=revenue_report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    @GetMapping("/export/bookings")
    public ResponseEntity<String> exportBookingsCSV() {
        String csvData = reportService.generateBookingsCSV();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bookings_report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
