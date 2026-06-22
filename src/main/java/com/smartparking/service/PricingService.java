package com.smartparking.service;

import com.smartparking.model.SlotType;
import com.smartparking.repository.ParkingSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

    private final ParkingSlotRepository parkingSlotRepository;
    private boolean dynamicPricingEnabled = true;
    private double peakOccupancyThreshold = 0.8;
    private double peakPriceMultiplier = 1.5;
    private double mediumOccupancyThreshold = 0.6;
    private double mediumPriceMultiplier = 1.25;
    private double discountOccupancyThreshold = 0.3;
    private double discountPriceMultiplier = 0.9;

    @Autowired
    public PricingService(ParkingSlotRepository parkingSlotRepository) {
        this.parkingSlotRepository = parkingSlotRepository;
    }

    public double getBaseHourlyRate(SlotType type) {
        switch (type) {
            case TWO_WHEELER:
                return 10.0;
            case FOUR_WHEELER:
                return 20.0;
            case EV:
                return 25.0;
            case DISABLED:
                return 15.0;
            default:
                return 15.0;
        }
    }

    public double getOccupancyRate() {
        long totalSlots = parkingSlotRepository.count();
        if (totalSlots == 0) return 0.0;
        long occupiedSlots = parkingSlotRepository.countByStatus(com.smartparking.model.SlotStatus.OCCUPIED) +
                             parkingSlotRepository.countByStatus(com.smartparking.model.SlotStatus.RESERVED);
        return (double) occupiedSlots / totalSlots;
    }

    public double calculateCurrentRate(SlotType type) {
        double baseRate = getBaseHourlyRate(type);
        if (!dynamicPricingEnabled) {
            return baseRate;
        }

        double occupancy = getOccupancyRate();

        if (occupancy >= peakOccupancyThreshold) {
            return baseRate * peakPriceMultiplier;
        } else if (occupancy >= mediumOccupancyThreshold) {
            return baseRate * mediumPriceMultiplier;
        } else if (occupancy <= discountOccupancyThreshold) {
            return baseRate * discountPriceMultiplier;
        }

        return baseRate;
    }

    public boolean isDynamicPricingEnabled() {
        return dynamicPricingEnabled;
    }

    public void setDynamicPricingEnabled(boolean enabled) {
        this.dynamicPricingEnabled = enabled;
    }

    public double getPeakOccupancyThreshold() {
        return peakOccupancyThreshold;
    }

    public void setPeakOccupancyThreshold(double threshold) {
        this.peakOccupancyThreshold = threshold;
    }

    public double getPeakPriceMultiplier() {
        return peakPriceMultiplier;
    }

    public void setPeakPriceMultiplier(double multiplier) {
        this.peakPriceMultiplier = multiplier;
    }

    public double getMediumOccupancyThreshold() {
        return mediumOccupancyThreshold;
    }

    public void setMediumOccupancyThreshold(double threshold) {
        this.mediumOccupancyThreshold = threshold;
    }

    public double getMediumPriceMultiplier() {
        return mediumPriceMultiplier;
    }

    public void setMediumPriceMultiplier(double multiplier) {
        this.mediumPriceMultiplier = multiplier;
    }

    public double getDiscountOccupancyThreshold() {
        return discountOccupancyThreshold;
    }

    public void setDiscountOccupancyThreshold(double threshold) {
        this.discountOccupancyThreshold = threshold;
    }

    public double getDiscountPriceMultiplier() {
        return discountPriceMultiplier;
    }

    public void setDiscountPriceMultiplier(double multiplier) {
        this.discountPriceMultiplier = multiplier;
    }
}
