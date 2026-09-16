package com.sharemile.repository;

import com.sharemile.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByPassengerIdOrderByCreatedAtDesc(Long passengerId);
    List<Booking> findByRideId(Long rideId);
    List<Booking> findByRideDriverIdOrderByCreatedAtDesc(Long driverId);
    List<Booking> findByStatus(String status);
}
