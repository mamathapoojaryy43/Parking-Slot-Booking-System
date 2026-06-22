package com.smartparking.service;

import com.smartparking.model.User;
import com.smartparking.model.Vehicle;
import com.smartparking.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Autowired
    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public Vehicle registerVehicle(Vehicle vehicle) {
        Optional<Vehicle> existing = vehicleRepository.findByLicensePlate(vehicle.getLicensePlate());
        if (existing.isPresent()) {
            throw new RuntimeException("Vehicle with this license plate is already registered!");
        }
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getVehiclesByOwner(User owner) {
        return vehicleRepository.findByOwnerId(owner.getId());
    }

    public Optional<Vehicle> getVehicleByLicensePlate(String licensePlate) {
        return vehicleRepository.findByLicensePlate(licensePlate);
    }

    public Optional<Vehicle> getVehicleById(Long id) {
        return vehicleRepository.findById(id);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        vehicleRepository.deleteById(id);
    }
}
