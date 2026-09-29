package com.sharemile.service;

import com.sharemile.dto.BookingRequest;
import com.sharemile.model.Booking;
import com.sharemile.model.Ride;
import com.sharemile.model.User;
import com.sharemile.repository.BookingRepository;
import com.sharemile.repository.RideRepository;
import com.sharemile.repository.UserRepository;
import com.sharemile.util.SpatialUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CommunicationService communicationService;

    public BookingService(BookingRepository bookingRepository,
                          RideRepository rideRepository,
                          UserRepository userRepository,
                          NotificationService notificationService,
                          CommunicationService communicationService) {
        this.bookingRepository = bookingRepository;
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.communicationService = communicationService;
    }

    /**
     * Executes atomic seat allocation with pessimistic row-level locking (SELECT ... FOR UPDATE).
     * This guarantees ACID compliance and strictly prevents double-booking race conditions
     * when multiple passengers concurrently attempt to reserve the final seats.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Booking bookSeatsAtomic(BookingRequest request, String passengerUsername) {
        User passenger = userRepository.findByUsername(passengerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found: " + passengerUsername));

        int seatsRequested = (request.getSeatsBooked() > 0) ? request.getSeatsBooked() : 1;

        // Acquire exclusive pessimistic write lock on the target ride record
        Ride ride = rideRepository.findByIdWithLock(request.getRideId())
                .orElseThrow(() -> new IllegalArgumentException("Ride not found with ID: " + request.getRideId()));

        if (ride.getDriver().getId().equals(passenger.getId())) {
            throw new IllegalArgumentException("Drivers cannot book seats on their own published rides!");
        }

        if (!"SCHEDULED".equalsIgnoreCase(ride.getStatus())) {
            throw new IllegalStateException("Ride is not currently open for reservations (Status: " + ride.getStatus() + ")");
        }

        if (ride.getAvailableSeats() < seatsRequested) {
            throw new IllegalStateException("Insufficient seat capacity! Only " + ride.getAvailableSeats() +
                    " seat(s) remaining for ride #" + ride.getId());
        }

        // Atomically decrement available seats
        ride.setAvailableSeats(ride.getAvailableSeats() - seatsRequested);
        rideRepository.save(ride);

        double totalFare = seatsRequested * ride.getPricePerSeat();

        // Calculate passenger's carbon offset savings: Distance * 0.12 * (passengers - 1)
        int currentTotalPassengers = (ride.getTotalSeats() - ride.getAvailableSeats()) + 1;
        double carbonOffset = SpatialUtils.calculateCarbonSavingsKg(ride.getEstimatedDistanceKm(), currentTotalPassengers);

        Booking booking = new Booking();
        booking.setRide(ride);
        booking.setPassenger(passenger);
        booking.setSeatsBooked(seatsRequested);
        booking.setTotalFare(totalFare);
        booking.setStatus("PENDING");
        booking.setPickupTitle(request.getPickupTitle() != null ? request.getPickupTitle() : ride.getOriginTitle());
        booking.setPickupLat(request.getPickupLat() != 0 ? request.getPickupLat() : ride.getOriginLat());
        booking.setPickupLng(request.getPickupLng() != 0 ? request.getPickupLng() : ride.getOriginLng());
        booking.setDropTitle(request.getDropTitle() != null ? request.getDropTitle() : ride.getDestTitle());
        booking.setDropLat(request.getDropLat() != 0 ? request.getDropLat() : ride.getDestLat());
        booking.setDropLng(request.getDropLng() != 0 ? request.getDropLng() : ride.getDestLng());
        booking.setPassengerNames(request.getPassengerNames() != null ? request.getPassengerNames() : passenger.getFullName());
        booking.setNote(request.getNote());
        booking.setCarbonOffsetKg(carbonOffset);

        Booking savedBooking = bookingRepository.save(booking);

        // Notify Driver via Email, SMS & WebSocket STOMP
        String driverMsg = String.format("%s (%s) requested %d seat(s) for your ride to %s (Co-travelers: %s). Please review and accept.",
                passenger.getFullName(),
                passenger.getGender() != null ? passenger.getGender() : "Not specified",
                seatsRequested,
                ride.getDestTitle(),
                booking.getPassengerNames());
        communicationService.notifyMultiChannel(ride.getDriver(), "New Booking Request (Pending Approval)", driverMsg, "BOOKING_REQUEST");

        // Notify Passenger via Email & SMS that request is sent
        String passengerMsg = String.format("Your reservation request for ride #%d to %s has been submitted. Awaiting driver %s's approval.",
                ride.getId(), ride.getDestTitle(), ride.getDriver().getFullName());
        communicationService.notifyMultiChannel(passenger, "Booking Request Submitted", passengerMsg, "BOOKING_REQUEST");

        return savedBooking;
    }

    @Transactional
    public Booking respondToBooking(Long bookingId, boolean accept, String driverUsername) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        Ride ride = booking.getRide();
        if (!ride.getDriver().getUsername().equals(driverUsername)) {
            throw new IllegalStateException("Only the assigned driver can accept or reject this booking request!");
        }

        if (!"PENDING".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Booking has already been processed with status: " + booking.getStatus());
        }

        if (accept) {
            booking.setStatus("ACCEPTED");
            String acceptMsg = String.format("Driver %s has confirmed your ride to %s! Your %d seat(s) are officially confirmed. Total: Rs. %.2f",
                    ride.getDriver().getFullName(), ride.getDestTitle(), booking.getSeatsBooked(), booking.getTotalFare());
            communicationService.notifyMultiChannel(booking.getPassenger(), "Ride Booking Confirmed!", acceptMsg, "BOOKING_ACCEPTED");

            // Also acknowledge driver
            communicationService.notifyMultiChannel(ride.getDriver(), "Booking Accepted",
                    "You approved booking #" + booking.getId() + " for " + booking.getPassenger().getFullName(), "BOOKING_CONFIRMED");
        } else {
            booking.setStatus("REJECTED");
            // Re-credit the reserved seats back into the available pool
            ride.setAvailableSeats(ride.getAvailableSeats() + booking.getSeatsBooked());
            rideRepository.save(ride);

            String declineMsg = String.format("Driver %s was unable to accommodate your booking request for ride to %s. Reserved seats have been released.",
                    ride.getDriver().getFullName(), ride.getDestTitle());
            communicationService.notifyMultiChannel(booking.getPassenger(), "Booking Declined", declineMsg, "BOOKING_REJECTED");
        }

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancelBooking(Long bookingId, String username) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        boolean isPassenger = booking.getPassenger().getUsername().equals(username);
        boolean isDriver = booking.getRide().getDriver().getUsername().equals(username);

        if (!isPassenger && !isDriver) {
            throw new IllegalStateException("You are not authorized to cancel this booking!");
        }

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus()) || "REJECTED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalStateException("Booking is already in inactive status: " + booking.getStatus());
        }

        // Restore seat capacity if booking was active or pending
        Ride ride = booking.getRide();
        ride.setAvailableSeats(ride.getAvailableSeats() + booking.getSeatsBooked());
        rideRepository.save(ride);

        booking.setStatus("CANCELLED");
        Booking saved = bookingRepository.save(booking);

        if (isPassenger) {
            notificationService.sendNotification(
                    ride.getDriver(),
                    "Booking Cancelled",
                    booking.getPassenger().getFullName() + " cancelled their booking on your ride to " + ride.getDestTitle(),
                    "RIDE_CANCELLED"
            );
        } else {
            notificationService.sendNotification(
                    booking.getPassenger(),
                    "Ride Cancelled by Driver",
                    "Driver " + ride.getDriver().getFullName() + " cancelled your ride to " + ride.getDestTitle(),
                    "RIDE_CANCELLED"
            );
        }

        return saved;
    }

    public List<Booking> getPassengerBookings(String passengerUsername) {
        User passenger = userRepository.findByUsername(passengerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Passenger not found"));
        return bookingRepository.findByPassengerIdOrderByCreatedAtDesc(passenger.getId());
    }

    public List<Booking> getDriverIncomingBookings(String driverUsername) {
        User driver = userRepository.findByUsername(driverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        return bookingRepository.findByRideDriverIdOrderByCreatedAtDesc(driver.getId());
    }
}
