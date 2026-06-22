package com.smartparking.repository;

import com.smartparking.model.Booking;
import com.smartparking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    Optional<Booking> findByBookingNumber(String bookingNumber);
    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);
    long countByStatus(BookingStatus status);
    List<Booking> findByStartTimeBeforeAndStatus(LocalDateTime time, BookingStatus status);
}
