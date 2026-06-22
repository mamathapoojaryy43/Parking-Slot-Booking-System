package com.smartparking.repository;

import com.smartparking.model.ParkingSlot;
import com.smartparking.model.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {
    List<ParkingSlot> findByFloorId(Long floorId);
    List<ParkingSlot> findByStatus(SlotStatus status);
    List<ParkingSlot> findByFloorIdAndStatus(Long floorId, SlotStatus status);
    Optional<ParkingSlot> findBySlotNumber(String slotNumber);
    long countByStatus(SlotStatus status);
    long countByFloorId(Long floorId);
    long countByFloorIdAndStatus(Long floorId, SlotStatus status);
}
