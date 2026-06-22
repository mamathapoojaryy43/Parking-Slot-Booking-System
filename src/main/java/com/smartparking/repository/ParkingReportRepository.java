package com.smartparking.repository;

import com.smartparking.model.ParkingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingReportRepository extends JpaRepository<ParkingReport, Long> {
    List<ParkingReport> findByDateBetween(LocalDate startDate, LocalDate endDate);
    Optional<ParkingReport> findByDate(LocalDate date);
}
