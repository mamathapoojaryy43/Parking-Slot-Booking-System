package com.smartparking.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "parking_reports")
public class ParkingReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "total_bookings", nullable = false)
    private Integer totalBookings = 0;

    @Column(name = "total_revenue", nullable = false)
    private Double totalRevenue = 0.0;

    @Column(name = "occupancy_rate", nullable = false)
    private Double occupancyRate = 0.0;

    public ParkingReport() {}

    public ParkingReport(LocalDate date, Integer totalBookings, Double totalRevenue, Double occupancyRate) {
        this.date = date;
        this.totalBookings = totalBookings;
        this.totalRevenue = totalRevenue;
        this.occupancyRate = occupancyRate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(Integer totalBookings) {
        this.totalBookings = totalBookings;
    }

    public Double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(Double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Double getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(Double occupancyRate) {
        this.occupancyRate = occupancyRate;
    }
}
