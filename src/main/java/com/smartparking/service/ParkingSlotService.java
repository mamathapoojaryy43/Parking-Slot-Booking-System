package com.smartparking.service;

import com.smartparking.model.*;
import com.smartparking.repository.FloorRepository;
import com.smartparking.repository.ParkingSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ParkingSlotService {

    private final ParkingSlotRepository parkingSlotRepository;
    private final FloorRepository floorRepository;

    @Autowired
    public ParkingSlotService(ParkingSlotRepository parkingSlotRepository, FloorRepository floorRepository) {
        this.parkingSlotRepository = parkingSlotRepository;
        this.floorRepository = floorRepository;
    }

    public List<Floor> getAllFloors() {
        return floorRepository.findAll();
    }

    public Optional<Floor> getFloorById(Long id) {
        return floorRepository.findById(id);
    }

    @Transactional
    public Floor saveFloor(Floor floor) {
        return floorRepository.save(floor);
    }

    public List<ParkingSlot> getSlotsByFloor(Long floorId) {
        return parkingSlotRepository.findByFloorId(floorId);
    }

    public Optional<ParkingSlot> getSlotByNumber(String slotNumber) {
        return parkingSlotRepository.findBySlotNumber(slotNumber);
    }

    public Optional<ParkingSlot> getSlotById(Long id) {
        return parkingSlotRepository.findById(id);
    }

    @Transactional
    public ParkingSlot saveSlot(ParkingSlot slot) {
        return parkingSlotRepository.save(slot);
    }

    public long getCountByStatus(SlotStatus status) {
        return parkingSlotRepository.countByStatus(status);
    }

    /**
     * AI-Based Best Slot Recommendation Algorithm
     * Heuristics:
     * 1. Matches slot type with vehicle type (or EV charging for EV, DISABLED for handicap accessibility request).
     * 2. Lower floors are closer to entry/exit points, saving search time and carbon emissions.
     * 3. Lower slot numbers on each floor are closest to elevators.
     */
    public Optional<ParkingSlot> recommendBestSlot(VehicleType vehicleType, boolean requiresDisabled) {
        List<ParkingSlot> availableSlots = parkingSlotRepository.findByStatus(SlotStatus.AVAILABLE);

        if (availableSlots.isEmpty()) {
            return Optional.empty();
        }

        // Determine target slot type
        SlotType targetType = SlotType.FOUR_WHEELER;
        if (requiresDisabled) {
            targetType = SlotType.DISABLED;
        } else if (vehicleType == VehicleType.EV) {
            targetType = SlotType.EV;
        } else if (vehicleType == VehicleType.TWO_WHEELER) {
            targetType = SlotType.TWO_WHEELER;
        }

        final SlotType finalTargetType = targetType;

        // Filter slots matching target type
        List<ParkingSlot> matchingSlots = availableSlots.stream()
                .filter(slot -> slot.getType() == finalTargetType)
                .collect(Collectors.toList());

        // Fallback to general Four-Wheeler slots if EV or Disabled slots are unavailable
        if (matchingSlots.isEmpty() && (finalTargetType == SlotType.EV || finalTargetType == SlotType.DISABLED)) {
            matchingSlots = availableSlots.stream()
                    .filter(slot -> slot.getType() == SlotType.FOUR_WHEELER)
                    .collect(Collectors.toList());
        }

        // Sort by floor number ascending (closer to ground level) and then by slot number string length/lexical order (closer to elevator)
        return matchingSlots.stream()
                .min(Comparator
                        .comparing((ParkingSlot s) -> s.getFloor().getFloorNumber())
                        .thenComparing(ParkingSlot::getSlotNumber)
                );
    }

    @Transactional
    public void deleteSlot(Long id) {
        parkingSlotRepository.deleteById(id);
    }

    @Transactional
    public void deleteFloor(Long id) {
        // First delete all slots on this floor
        List<ParkingSlot> slots = parkingSlotRepository.findByFloorId(id);
        parkingSlotRepository.deleteAll(slots);
        floorRepository.deleteById(id);
    }
}
