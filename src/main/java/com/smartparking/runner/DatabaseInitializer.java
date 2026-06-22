package com.smartparking.runner;

import com.smartparking.model.*;
import com.smartparking.repository.*;
import com.smartparking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final FloorRepository floorRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final EVChargingStationRepository evChargingStationRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final ParkingReportRepository parkingReportRepository;
    private final VehicleRepository vehicleRepository;

    @Autowired
    public DatabaseInitializer(RoleRepository roleRepository, UserService userService, UserRepository userRepository,
                               FloorRepository floorRepository, ParkingSlotRepository parkingSlotRepository,
                               EVChargingStationRepository evChargingStationRepository, BookingRepository bookingRepository,
                               PaymentRepository paymentRepository, NotificationRepository notificationRepository,
                               ParkingReportRepository parkingReportRepository, VehicleRepository vehicleRepository) {
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.floorRepository = floorRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.evChargingStationRepository = evChargingStationRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
        this.parkingReportRepository = parkingReportRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Roles setup
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role("ROLE_USER"));
            roleRepository.save(new Role("ROLE_ADMIN"));
        }

        // 2. Users setup
        User adminUser = null;
        User normalUser = null;
        if (userRepository.count() == 0) {
            User admin = new User("admin", "admin123", "admin@smartparking.com", "System Admin", "9876543210");
            adminUser = userService.registerAdmin(admin);

            User user = new User("user", "user123", "user@smartparking.com", "John Doe", "9876543211");
            normalUser = userService.registerUser(user);
        } else {
            adminUser = userRepository.findByUsername("admin").orElse(null);
            normalUser = userRepository.findByUsername("user").orElse(null);
        }

        // 3. Vehicles setup
        Vehicle userVehicle = null;
        if (normalUser != null && vehicleRepository.count() == 0) {
            userVehicle = vehicleRepository.save(new Vehicle("KA-01-ME-1234", VehicleType.EV, "Tesla Model 3", normalUser));
            vehicleRepository.save(new Vehicle("KA-01-ME-5678", VehicleType.FOUR_WHEELER, "Hyundai i20", normalUser));
            vehicleRepository.save(new Vehicle("KA-01-ME-9999", VehicleType.TWO_WHEELER, "Royal Enfield Bullet", normalUser));
        } else if (normalUser != null) {
            List<Vehicle> list = vehicleRepository.findByOwnerId(normalUser.getId());
            if (!list.isEmpty()) userVehicle = list.get(0);
        }

        // 4. Floors setup
        Floor ground = null;
        Floor floor1 = null;
        Floor floor2 = null;
        if (floorRepository.count() == 0) {
            ground = floorRepository.save(new Floor(0, "Ground Floor", 15));
            floor1 = floorRepository.save(new Floor(1, "Floor 1", 15));
            floor2 = floorRepository.save(new Floor(2, "Floor 2", 15));
        } else {
            ground = floorRepository.findByFloorNumber(0).orElse(null);
            floor1 = floorRepository.findByFloorNumber(1).orElse(null);
            floor2 = floorRepository.findByFloorNumber(2).orElse(null);
        }

        // 5. Parking slots and EV stations setup
        if (parkingSlotRepository.count() == 0 && ground != null && floor1 != null && floor2 != null) {
            // Ground Floor (Two-Wheelers & Disabled)
            for (int i = 1; i <= 15; i++) {
                String slotNum = "G-" + String.format("%02d", i);
                SlotType type = (i >= 6 && i <= 10) ? SlotType.DISABLED : SlotType.TWO_WHEELER;
                parkingSlotRepository.save(new ParkingSlot(slotNum, type, SlotStatus.AVAILABLE, ground));
            }

            // Floor 1 (EV Charging & Four-Wheelers)
            for (int i = 1; i <= 15; i++) {
                String slotNum = "F1-" + String.format("%02d", i);
                SlotType type = (i <= 5) ? SlotType.EV : SlotType.FOUR_WHEELER;
                ParkingSlot slot = parkingSlotRepository.save(new ParkingSlot(slotNum, type, SlotStatus.AVAILABLE, floor1));
                
                // Add EV charging station for EV slots
                if (type == SlotType.EV) {
                    evChargingStationRepository.save(new EVChargingStation(slot, ChargingStatus.AVAILABLE, 0.15)); // $0.15 per min
                }
            }

            // Floor 2 (Four-Wheelers)
            for (int i = 1; i <= 15; i++) {
                String slotNum = "F2-" + String.format("%02d", i);
                parkingSlotRepository.save(new ParkingSlot(slotNum, SlotType.FOUR_WHEELER, SlotStatus.AVAILABLE, floor2));
            }
        }

        // 6. Setup mock reports and booking histories for Admin Panel Charts
        if (parkingReportRepository.count() == 0) {
            LocalDate today = LocalDate.now();
            Random rand = new Random();
            for (int i = 7; i >= 1; i--) {
                LocalDate date = today.minusDays(i);
                int bookingsCount = 15 + rand.nextInt(15); // 15 to 30 bookings
                double revenue = bookingsCount * (30 + rand.nextInt(20)); // $30-50 per booking average
                double occupancy = 45.0 + rand.nextDouble() * 35.0; // 45% to 80% occupancy
                parkingReportRepository.save(new ParkingReport(date, bookingsCount, revenue, occupancy));
            }
        }

        // 7. Setup mock notifications
        if (normalUser != null && notificationRepository.findByUserIdOrderByCreatedAtDesc(normalUser.getId()).isEmpty()) {
            notificationRepository.save(new Notification(normalUser, "Welcome to the Smart Parking System! Register your vehicles and book slots online."));
            notificationRepository.save(new Notification(normalUser, "Eco savings tip: Choosing optimized spots reduces search time and carbon footprint by up to 80%!"));
        }

        // 8. Place a default active booking for testing if possible
        if (normalUser != null && userVehicle != null && bookingRepository.count() == 0) {
            Optional<ParkingSlot> optSlot = parkingSlotRepository.findBySlotNumber("F1-01");
            if (optSlot.isPresent()) {
                ParkingSlot slot = optSlot.get();
                slot.setStatus(SlotStatus.OCCUPIED);
                parkingSlotRepository.save(slot);

                Optional<EVChargingStation> charger = evChargingStationRepository.findBySlotId(slot.getId());
                if (charger.isPresent()) {
                    EVChargingStation st = charger.get();
                    st.setStatus(ChargingStatus.CHARGING);
                    evChargingStationRepository.save(st);
                }

                Booking activeBooking = new Booking(
                        "B-" + System.currentTimeMillis() % 1000000,
                        normalUser,
                        slot,
                        userVehicle,
                        LocalDateTime.now().minusHours(2),
                        null,
                        BookingStatus.ACTIVE,
                        40.0,
                        "QR-ACTIVE-MOCK"
                );
                bookingRepository.save(activeBooking);

                Payment payment = new Payment(
                        activeBooking,
                        40.0,
                        LocalDateTime.now().minusHours(2),
                        PaymentStatus.COMPLETED,
                        "CARD",
                        "TXN-" + System.currentTimeMillis() % 1000000
                );
                paymentRepository.save(payment);
            }
        }
    }
}
