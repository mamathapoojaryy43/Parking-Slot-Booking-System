package com.smartparking.service;

import com.smartparking.model.*;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.EVChargingStationRepository;
import com.smartparking.repository.ParkingSlotRepository;
import com.smartparking.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final EVChargingStationRepository evChargingStationRepository;
    private final PaymentRepository paymentRepository;
    private final PricingService pricingService;
    private final NotificationService notificationService;

    @Autowired
    public BookingService(BookingRepository bookingRepository, ParkingSlotRepository parkingSlotRepository,
                          EVChargingStationRepository evChargingStationRepository, PaymentRepository paymentRepository,
                          PricingService pricingService, NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.evChargingStationRepository = evChargingStationRepository;
        this.paymentRepository = paymentRepository;
        this.pricingService = pricingService;
        this.notificationService = notificationService;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public Optional<Booking> getBookingByNumber(String bookingNumber) {
        return bookingRepository.findByBookingNumber(bookingNumber);
    }

    public Optional<Booking> getActiveBookingBySlot(String slotNumber) {
        return bookingRepository.findByStatus(BookingStatus.ACTIVE).stream()
                .filter(b -> b.getSlot().getSlotNumber().equalsIgnoreCase(slotNumber))
                .findFirst();
    }

    @Transactional
    public Booking reserveSlot(User user, ParkingSlot slot, Vehicle vehicle, LocalDateTime startTime) {
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new RuntimeException("Selected slot is not available for reservation!");
        }

        // Lock slot as reserved
        slot.setStatus(SlotStatus.RESERVED);
        parkingSlotRepository.save(slot);

        String bookingNumber = "B-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String qrData = "PARK-" + bookingNumber + "-" + slot.getSlotNumber();

        Booking booking = new Booking(
                bookingNumber,
                user,
                slot,
                vehicle,
                startTime,
                null,
                BookingStatus.RESERVED,
                0.0, // Calculated at exit checkout, or temporary base deposit
                qrData
        );

        Booking savedBooking = bookingRepository.save(booking);

        // Notify user
        notificationService.sendNotification(user,
                "Reservation confirmed! Booking #" + bookingNumber + " for Slot " + slot.getSlotNumber() + " at " + startTime);

        return savedBooking;
    }

    @Transactional
    public Booking checkIn(String bookingNumber) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new RuntimeException("Booking not found!"));

        if (booking.getStatus() != BookingStatus.RESERVED) {
            throw new RuntimeException("Booking is not in RESERVED status!");
        }

        ParkingSlot slot = booking.getSlot();
        slot.setStatus(SlotStatus.OCCUPIED);
        parkingSlotRepository.save(slot);

        // If EV slot, update charging status
        if (slot.getType() == SlotType.EV) {
            evChargingStationRepository.findBySlotId(slot.getId()).ifPresent(charger -> {
                charger.setStatus(ChargingStatus.CHARGING);
                evChargingStationRepository.save(charger);
            });
        }

        booking.setStatus(BookingStatus.ACTIVE);
        booking.setStartTime(LocalDateTime.now()); // Reset start time to actual check-in time
        Booking savedBooking = bookingRepository.save(booking);

        notificationService.sendNotification(booking.getUser(),
                "Vehicle checked in successfully at Slot " + slot.getSlotNumber() + ". Parking timer started.");

        return savedBooking;
    }

    @Transactional
    public Booking checkOut(String bookingNumber, String paymentMethod) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new RuntimeException("Booking not found!"));

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new RuntimeException("Booking is not currently ACTIVE!");
        }

        ParkingSlot slot = booking.getSlot();
        LocalDateTime exitTime = LocalDateTime.now();
        booking.setEndTime(exitTime);

        // Calculate hours parked (minimum 1 hour)
        long minutes = Duration.between(booking.getStartTime(), exitTime).toMinutes();
        double hours = Math.max(1.0, Math.ceil(minutes / 60.0));

        // Cost calculation
        double hourlyRate = pricingService.calculateCurrentRate(slot.getType());
        double totalCost = hours * hourlyRate;

        // If EV, calculate charging rate
        if (slot.getType() == SlotType.EV) {
            Optional<EVChargingStation> chargerOpt = evChargingStationRepository.findBySlotId(slot.getId());
            if (chargerOpt.isPresent()) {
                EVChargingStation charger = chargerOpt.get();
                // Add charging cost (e.g. charging rate per minute of active stay)
                double chargingCost = minutes * charger.getChargingRate();
                totalCost += chargingCost;

                // Reset charger status
                charger.setStatus(ChargingStatus.AVAILABLE);
                evChargingStationRepository.save(charger);
            }
        }

        booking.setTotalAmount(totalCost);
        booking.setStatus(BookingStatus.COMPLETED);
        Booking savedBooking = bookingRepository.save(booking);

        // Release slot
        slot.setStatus(SlotStatus.AVAILABLE);
        parkingSlotRepository.save(slot);

        // Create Completed Payment Transaction
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        Payment payment = new Payment(
                savedBooking,
                totalCost,
                exitTime,
                PaymentStatus.COMPLETED,
                paymentMethod,
                transactionId
        );
        paymentRepository.save(payment);

        // Green Math: Carbon Emission Tracking
        // Standard search time for a spot is ~7 minutes. Finding it immediately reduces search to 1 minute.
        // 6 minutes of idling saved. Idling emits ~150g CO2 per minute.
        // Total saved: 6 * 150 = 900g CO2.
        // For EV, we also track standard emissions offset compared to combustion engines (e.g. 200g CO2 saved per mile/hour).
        long co2SavedGrams = 900;
        if (slot.getType() == SlotType.EV) {
            co2SavedGrams += (long) (hours * 1500); // Additional EV offsets
        }

        String greenMsg = " Your quick parking saved " + co2SavedGrams + "g of CO2 emissions! Green points earned.";

        notificationService.sendNotification(booking.getUser(),
                "Receipt generated for Booking #" + booking.getBookingNumber() +
                ". Paid ₹" + String.format("%.2f", totalCost) + " via " + paymentMethod + "." + greenMsg);

        return savedBooking;
    }

    /**
     * Auto Release Scheduler:
     * Runs every 60 seconds (1 minute).
     * Finds bookings in RESERVED status where the startTime has passed by more than 30 minutes.
     * Releases those slots automatically.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseUnusedReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<Booking> expiredBookings = bookingRepository.findByStartTimeBeforeAndStatus(cutoff, BookingStatus.RESERVED);

        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            ParkingSlot slot = booking.getSlot();
            slot.setStatus(SlotStatus.AVAILABLE);
            parkingSlotRepository.save(slot);

            notificationService.sendNotification(booking.getUser(),
                    "Reservation booking #" + booking.getBookingNumber() + " expired and was automatically released because you did not check in within 30 minutes.");
        }
    }
}
