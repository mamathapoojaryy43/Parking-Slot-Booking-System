package com.smartparking.model;

import jakarta.persistence.*;

@Entity
@Table(name = "floors")
public class Floor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "floor_number", unique = true, nullable = false)
    private Integer floorNumber;

    @Column(name = "floor_name", nullable = false)
    private String floorName;

    @Column(nullable = false)
    private Integer capacity;

    public Floor() {}

    public Floor(Integer floorNumber, String floorName, Integer capacity) {
        this.floorNumber = floorNumber;
        this.floorName = floorName;
        this.capacity = capacity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}
