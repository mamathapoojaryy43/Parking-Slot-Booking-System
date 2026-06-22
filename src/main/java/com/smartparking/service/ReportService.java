package com.smartparking.service;

import com.smartparking.model.*;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.ParkingReportRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {

    private final ParkingReportRepository parkingReportRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ParkingSlotRepository parkingSlotRepository;

    @Autowired
    public ReportService(ParkingReportRepository parkingReportRepository, BookingRepository bookingRepository,
                         PaymentRepository paymentRepository, ParkingSlotRepository parkingSlotRepository) {
        this.parkingReportRepository = parkingReportRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.parkingSlotRepository = parkingSlotRepository;
    }

    public List<ParkingReport> getReportsForLastDays(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);
        return parkingReportRepository.findByDateBetween(start, end);
    }

    @Transactional
    public ParkingReport generateDailySummary(LocalDate date) {
        // Calculate total bookings today
        List<Booking> bookings = bookingRepository.findAll();
        int count = 0;
        double revenue = 0.0;

        for (Booking b : bookings) {
            LocalDateTime start = b.getStartTime();
            if (start != null && start.toLocalDate().equals(date)) {
                count++;
                if (b.getTotalAmount() != null) {
                    revenue += b.getTotalAmount();
                }
            }
        }

        // Calculate average occupancy rate
        long totalSlots = parkingSlotRepository.count();
        double occupancy = 0.0;
        if (totalSlots > 0) {
            long occupiedAndReserved = parkingSlotRepository.countByStatus(SlotStatus.OCCUPIED) +
                                       parkingSlotRepository.countByStatus(SlotStatus.RESERVED);
            occupancy = ((double) occupiedAndReserved / totalSlots) * 100.0;
        }

        // Save report
        Optional<ParkingReport> existing = parkingReportRepository.findByDate(date);
        ParkingReport report = existing.orElse(new ParkingReport());
        report.setDate(date);
        report.setTotalBookings(count);
        report.setTotalRevenue(revenue);
        report.setOccupancyRate(occupancy);

        return parkingReportRepository.save(report);
    }

    /**
     * Smart Feature: Peak-Hour Occupancy Prediction
     * Heuristics based on day of week:
     * - Weekdays (Mon-Fri): Peaks around 9-11 AM (commuters check-in) and 2-4 PM (afternoon meetings).
     * - Weekends (Sat-Sun): Peaks around 1-3 PM (lunch/shoppers) and 6-9 PM (dinner/moviegoers).
     */
    public Map<Integer, Double> predictHourlyOccupancy(LocalDate date) {
        Map<Integer, Double> predictions = new LinkedHashMap<>();
        DayOfWeek day = date.getDayOfWeek();
        boolean isWeekend = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);

        for (int hour = 0; hour < 24; hour++) {
            double rate;
            if (isWeekend) {
                if (hour >= 18 && hour <= 21) {
                    rate = 85.0 + Math.random() * 10.0; // Evening peak
                } else if (hour >= 13 && hour <= 15) {
                    rate = 70.0 + Math.random() * 15.0; // Lunch peak
                } else if (hour >= 9 && hour <= 12) {
                    rate = 40.0 + Math.random() * 15.0;
                } else if (hour >= 0 && hour <= 6) {
                    rate = 10.0 + Math.random() * 10.0; // Night
                } else {
                    rate = 50.0 + Math.random() * 15.0;
                }
            } else {
                // Weekday
                if (hour >= 9 && hour <= 11) {
                    rate = 80.0 + Math.random() * 15.0; // Morning commuter peak
                } else if (hour >= 14 && hour <= 16) {
                    rate = 75.0 + Math.random() * 15.0; // Afternoon peak
                } else if (hour >= 12 && hour <= 13) {
                    rate = 60.0 + Math.random() * 10.0;
                } else if (hour >= 18 && hour <= 20) {
                    rate = 55.0 + Math.random() * 15.0;
                } else if (hour >= 0 && hour <= 6) {
                    rate = 15.0 + Math.random() * 10.0;
                } else {
                    rate = 45.0 + Math.random() * 15.0;
                }
            }
            predictions.put(hour, Math.round(rate * 10.0) / 10.0);
        }
        return predictions;
    }

    /**
     * Eco Tracking: Calculate total carbon offset in grams across all completed bookings
     */
    public long calculateTotalCO2Saved() {
        List<Booking> bookings = bookingRepository.findByStatus(BookingStatus.COMPLETED);
        long totalSaved = 0;
        for (Booking b : bookings) {
            long co2 = 900; // Base: 6 mins of search idling avoided
            if (b.getSlot().getType() == SlotType.EV) {
                // EV offset: additional 1500g per hour
                if (b.getStartTime() != null && b.getEndTime() != null) {
                    long hours = Duration.between(b.getStartTime(), b.getEndTime()).toHours();
                    co2 += Math.max(1, hours) * 1500;
                }
            }
            totalSaved += co2;
        }
        return totalSaved;
    }

    public String generateRevenueCSV() {
        List<ParkingReport> reports = parkingReportRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("Date,Total Bookings,Total Revenue (₹),Average Occupancy Rate (%)\n");
        for (ParkingReport r : reports) {
            csv.append(r.getDate().toString()).append(",")
               .append(r.getTotalBookings()).append(",")
               .append(String.format("%.2f", r.getTotalRevenue())).append(",")
               .append(String.format("%.1f", r.getOccupancyRate())).append("\n");
        }
        return csv.toString();
    }

    public String generateBookingsCSV() {
        List<Booking> bookings = bookingRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("Booking Number,User,Vehicle Plate,Slot Number,Slot Floor,Start Time,End Time,Status,Amount (₹)\n");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Booking b : bookings) {
            String start = b.getStartTime() != null ? b.getStartTime().format(dtf) : "N/A";
            String end = b.getEndTime() != null ? b.getEndTime().format(dtf) : "N/A";
            csv.append(b.getBookingNumber()).append(",")
               .append(b.getUser().getUsername()).append(",")
               .append(b.getVehicle().getLicensePlate()).append(",")
               .append(b.getSlot().getSlotNumber()).append(",")
               .append(b.getSlot().getFloor().getFloorName()).append(",")
               .append(start).append(",")
               .append(end).append(",")
               .append(b.getStatus().toString()).append(",")
               .append(String.format("%.2f", b.getTotalAmount() != null ? b.getTotalAmount() : 0.0)).append("\n");
        }
        return csv.toString();
    }
}
