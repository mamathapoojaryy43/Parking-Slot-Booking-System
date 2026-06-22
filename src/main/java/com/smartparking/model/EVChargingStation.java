package com.smartparking.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ev_charging_stations")
public class EVChargingStation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "slot_id", nullable = false)
    private ParkingSlot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargingStatus status = ChargingStatus.AVAILABLE;

    @Column(name = "charging_rate", nullable = false)
    private Double chargingRate;

    public EVChargingStation() {}

    public EVChargingStation(ParkingSlot slot, ChargingStatus status, Double chargingRate) {
        this.slot = slot;
        this.status = status;
        this.chargingRate = chargingRate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ParkingSlot getSlot() {
        return slot;
    }

    public void setSlot(ParkingSlot slot) {
        this.slot = slot;
    }

    public ChargingStatus getStatus() {
        return status;
    }

    public void setStatus(ChargingStatus status) {
        this.status = status;
    }

    public Double getChargingRate() {
        return chargingRate;
    }

    public void setChargingRate(Double chargingRate) {
        this.chargingRate = chargingRate;
    }
}
