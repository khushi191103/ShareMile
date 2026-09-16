package com.sharemile.config;

import com.sharemile.model.Booking;
import com.sharemile.model.Ride;
import com.sharemile.model.User;
import com.sharemile.repository.BookingRepository;
import com.sharemile.repository.RideRepository;
import com.sharemile.repository.UserRepository;
import com.sharemile.util.SpatialUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           RideRepository rideRepository,
                           BookingRepository bookingRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // Data already seeded
        }

        // 1. Seed Users
        User admin = new User("admin", "admin@sharemile.com", passwordEncoder.encode("admin123"), "System Administrator", "9876543210", "ROLE_ADMIN");
        admin.setVerified(true);
        userRepository.save(admin);

        User driver1 = new User("raj_driver", "raj@sharemile.com", passwordEncoder.encode("driver123"), "Rajesh Sharma", "9876543211", "ROLE_DRIVER");
        driver1.setVehicleModel("Honda City (White)");
        driver1.setVehicleNumber("MH-12-AB-1234");
        driver1.setVerified(true);
        driver1.setAverageRating(4.9);
        driver1.setTotalRatings(24);
        userRepository.save(driver1);

        User driver2 = new User("amit_driver", "amit@sharemile.com", passwordEncoder.encode("driver123"), "Amit Deshmukh", "9876543212", "ROLE_DRIVER");
        driver2.setVehicleModel("Hyundai Verna (Silver)");
        driver2.setVehicleNumber("MH-14-CD-5678");
        driver2.setVerified(true);
        driver2.setAverageRating(4.7);
        driver2.setTotalRatings(15);
        userRepository.save(driver2);

        User passenger1 = new User("khushi_passenger", "khushisingh.av@gmail.com", passwordEncoder.encode("pass123"), "Khushi Singh", "9695073789", "ROLE_PASSENGER");
        passenger1.setVerified(true);
        passenger1.setAverageRating(5.0);
        passenger1.setTotalRatings(8);
        userRepository.save(passenger1);

        User passenger2 = new User("sneha_passenger", "sneha@sharemile.com", passwordEncoder.encode("pass123"), "Sneha Patel", "9876543214", "ROLE_PASSENGER");
        passenger2.setVerified(true);
        passenger2.setAverageRating(4.8);
        passenger2.setTotalRatings(6);
        userRepository.save(passenger2);

        // 2. Seed Realistic Smart-City Commute Rides (Pune / PCMC Metro Area)
        // Ride 1: Hinjawadi Phase 1 to Shivajinagar
        Ride ride1 = new Ride();
        ride1.setDriver(driver1);
        ride1.setOriginTitle("Hinjawadi Phase 1 IT Park, Pune");
        ride1.setOriginLat(18.5913);
        ride1.setOriginLng(73.7389);
        ride1.setDestTitle("Shivajinagar Station, Pune");
        ride1.setDestLat(18.5314);
        ride1.setDestLng(73.8446);
        ride1.setDepartureTime(LocalDateTime.now().plusHours(3));
        ride1.setTotalSeats(4);
        ride1.setAvailableSeats(2);
        ride1.setPricePerSeat(80.0);
        ride1.setStatus("SCHEDULED");
        double dist1 = SpatialUtils.haversineDistanceKm(18.5913, 73.7389, 18.5314, 73.8446);
        ride1.setEstimatedDistanceKm(dist1);
        ride1.setEstimatedDurationMin(35);
        rideRepository.save(ride1);

        // Ride 2: Wakad to Magarpatta City
        Ride ride2 = new Ride();
        ride2.setDriver(driver2);
        ride2.setOriginTitle("Wakad Bridge, Pune");
        ride2.setOriginLat(18.5987);
        ride2.setOriginLng(73.7645);
        ride2.setDestTitle("Magarpatta Cybercity, Hadapsar");
        ride2.setDestLat(18.5144);
        ride2.setDestLng(73.9260);
        ride2.setDepartureTime(LocalDateTime.now().plusHours(5));
        ride2.setTotalSeats(3);
        ride2.setAvailableSeats(3);
        ride2.setPricePerSeat(120.0);
        ride2.setStatus("SCHEDULED");
        double dist2 = SpatialUtils.haversineDistanceKm(18.5987, 73.7645, 18.5144, 73.9260);
        ride2.setEstimatedDistanceKm(dist2);
        ride2.setEstimatedDurationMin(45);
        rideRepository.save(ride2);

        // Ride 3: Nigdi (ATSS IICMR) to Pune Railway Station
        Ride ride3 = new Ride();
        ride3.setDriver(driver1);
        ride3.setOriginTitle("ATSS IICMR Nigdi, Pradhikaran");
        ride3.setOriginLat(18.6517);
        ride3.setOriginLng(73.7716);
        ride3.setDestTitle("Pune Railway Station");
        ride3.setDestLat(18.5284);
        ride3.setDestLng(73.8739);
        ride3.setDepartureTime(LocalDateTime.now().plusDays(1).withHour(8).withMinute(30));
        ride3.setTotalSeats(4);
        ride3.setAvailableSeats(3);
        ride3.setPricePerSeat(95.0);
        ride3.setStatus("SCHEDULED");
        double dist3 = SpatialUtils.haversineDistanceKm(18.6517, 73.7716, 18.5284, 73.8739);
        ride3.setEstimatedDistanceKm(dist3);
        ride3.setEstimatedDurationMin(40);
        rideRepository.save(ride3);

        // 3. Seed Confirmed Sample Booking
        Booking booking1 = new Booking();
        booking1.setRide(ride1);
        booking1.setPassenger(passenger1);
        booking1.setSeatsBooked(2);
        booking1.setTotalFare(160.0);
        booking1.setStatus("ACCEPTED");
        booking1.setPickupTitle("Wakad Flyover");
        booking1.setPickupLat(18.5987);
        booking1.setPickupLng(73.7645);
        booking1.setDropTitle("Shivajinagar Court");
        booking1.setDropLat(18.5314);
        booking1.setDropLng(73.8446);
        booking1.setCarbonOffsetKg(SpatialUtils.calculateCarbonSavingsKg(dist1, 3));
        bookingRepository.save(booking1);
    }
}
