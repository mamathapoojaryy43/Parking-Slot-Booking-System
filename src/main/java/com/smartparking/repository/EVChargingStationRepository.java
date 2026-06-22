package com.smartparking.repository;

import com.smartparking.model.ChargingStatus;
import com.smartparking.model.EVChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EVChargingStationRepository extends JpaRepository<EVChargingStation, Long> {
    Optional<EVChargingStation> findBySlotId(Long slotId);
    List<EVChargingStation> findByStatus(ChargingStatus status);
}
