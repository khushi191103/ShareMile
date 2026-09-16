package com.sharemile.controller;

import com.sharemile.dto.BookingRequest;
import com.sharemile.model.Booking;
import com.sharemile.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<Booking> bookSeats(@RequestBody BookingRequest request, Authentication authentication) {
        Booking booking = bookingService.bookSeatsAtomic(request, authentication.getName());
        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{id}/respond")
    public ResponseEntity<Booking> respondBooking(@PathVariable Long id,
                                                  @RequestBody Map<String, Boolean> payload,
                                                  Authentication authentication) {
        boolean accept = payload.getOrDefault("accept", false);
        Booking updated = bookingService.respondToBooking(id, accept, authentication.getName());
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Booking> cancelBooking(@PathVariable Long id, Authentication authentication) {
        Booking cancelled = bookingService.cancelBooking(id, authentication.getName());
        return ResponseEntity.ok(cancelled);
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<Booking>> getMyBookings(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getPassengerBookings(authentication.getName()));
    }

    @GetMapping("/driver-requests")
    public ResponseEntity<List<Booking>> getDriverRequests(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getDriverIncomingBookings(authentication.getName()));
    }
}
