package com.sharemile;

import com.sharemile.dto.BookingRequest;
import com.sharemile.model.Booking;
import com.sharemile.model.Ride;
import com.sharemile.model.User;
import com.sharemile.repository.BookingRepository;
import com.sharemile.repository.RideRepository;
import com.sharemile.repository.UserRepository;
import com.sharemile.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private Long testRideId;
    private final List<String> passengerUsernames = new ArrayList<>();

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        rideRepository.deleteAll();

        // 1. Create a Driver
        User driver = userRepository.findByUsername("driver_test").orElseGet(() -> {
            User d = new User("driver_test", "driver_test@sharemile.com", "pass", "Test Driver", "111", "ROLE_DRIVER");
            return userRepository.save(d);
        });

        // 2. Create a Ride with ONLY 1 AVAILABLE SEAT
        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setOriginTitle("Origin");
        ride.setOriginLat(18.5);
        ride.setOriginLng(73.8);
        ride.setDestTitle("Destination");
        ride.setDestLat(18.6);
        ride.setDestLng(73.9);
        ride.setDepartureTime(LocalDateTime.now().plusDays(1));
        ride.setTotalSeats(1);
        ride.setAvailableSeats(1); // EXACTLY 1 SEAT!
        ride.setPricePerSeat(50.0);
        ride.setStatus("SCHEDULED");
        ride.setEstimatedDistanceKm(15.0);
        Ride savedRide = rideRepository.save(ride);
        testRideId = savedRide.getId();

        // 3. Create 5 separate passenger accounts
        passengerUsernames.clear();
        for (int i = 1; i <= 5; i++) {
            String uname = "concurrency_passenger_" + i;
            passengerUsernames.add(uname);
            if (!userRepository.existsByUsername(uname)) {
                User p = new User(uname, uname + "@sharemile.com", "pass", "Passenger " + i, "999", "ROLE_PASSENGER");
                userRepository.save(p);
            }
        }
    }

    @Test
    @DisplayName("Pessimistic row locking should prevent double-booking when 5 threads contest for 1 seat")
    void testConcurrentBookingSeatRaceCondition() throws InterruptedException {
        int numberOfThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> errorMessages = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            final String passengerUsername = passengerUsernames.get(i);
            executor.submit(() -> {
                try {
                    startSignal.await(); // Wait for all threads to align at starting line
                    BookingRequest request = new BookingRequest();
                    request.setRideId(testRideId);
                    request.setSeatsBooked(1);
                    bookingService.bookSeatsAtomic(request, passengerUsername);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    errorMessages.add(e.getMessage());
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        // Fire all 5 requests simultaneously
        startSignal.countDown();
        doneSignal.await();
        executor.shutdown();

        // ASSERTIONS:
        // 1. Exactly ONE passenger must have succeeded
        assertEquals(1, successCount.get(), "Only 1 passenger should successfully book the last seat");

        // 2. Exactly 4 passengers must have failed due to insufficient seats
        assertEquals(4, failureCount.get(), "The other 4 concurrent requests must be rejected");

        // 3. Check final ride state in database: availableSeats must be strictly 0 (never negative)
        Ride finalRide = rideRepository.findById(testRideId).orElseThrow();
        assertEquals(0, finalRide.getAvailableSeats(), "Remaining seats must be strictly 0, proving no overbooking occurred");

        // 4. Check total bookings created in DB
        List<Booking> createdBookings = bookingRepository.findByRideId(testRideId);
        assertEquals(1, createdBookings.size(), "Only 1 booking record should be persisted in database");
    }
}
