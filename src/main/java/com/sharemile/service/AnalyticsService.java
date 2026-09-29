package com.sharemile.service;

import com.sharemile.dto.AnalyticsResponse;
import com.sharemile.model.Booking;
import com.sharemile.model.Ride;
import com.sharemile.model.User;
import com.sharemile.repository.BookingRepository;
import com.sharemile.repository.RideRepository;
import com.sharemile.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;

    public AnalyticsService(UserRepository userRepository,
                            RideRepository rideRepository,
                            BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
    }

    public AnalyticsResponse getPlatformAnalytics() {
        AnalyticsResponse response = new AnalyticsResponse();

        List<User> users = userRepository.findAll();
        List<Ride> rides = rideRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();

        response.setTotalUsers(users.size());
        response.setVerifiedUsers(users.stream().filter(User::isVerified).count());
        response.setTotalDrivers(users.stream().filter(u -> (u.getVehicleModel() != null && !u.getVehicleModel().isBlank()) || "ROLE_DRIVER".equalsIgnoreCase(u.getRole())).count());
        response.setTotalPassengers(users.stream().filter(u -> !"ROLE_ADMIN".equalsIgnoreCase(u.getRole())).count());

        response.setTotalRidesPublished(rides.size());
        response.setActiveRides(rides.stream().filter(r -> "SCHEDULED".equalsIgnoreCase(r.getStatus())).count());
        response.setCompletedRides(rides.stream().filter(r -> "COMPLETED".equalsIgnoreCase(r.getStatus())).count());

        response.setTotalBookings(bookings.size());
        response.setAcceptedBookings(bookings.stream().filter(b -> "ACCEPTED".equalsIgnoreCase(b.getStatus())).count());
        response.setCancelledBookings(bookings.stream().filter(b -> "CANCELLED".equalsIgnoreCase(b.getStatus())).count());

        // Calculate platform carbon footprint offset
        double totalCarbon = bookings.stream()
                .filter(b -> "ACCEPTED".equalsIgnoreCase(b.getStatus()))
                .mapToDouble(Booking::getCarbonOffsetKg)
                .sum();
        response.setTotalCarbonOffsetKg(Math.round(totalCarbon * 10.0) / 10.0);

        // Total transaction fare volume
        double totalFare = bookings.stream()
                .filter(b -> "ACCEPTED".equalsIgnoreCase(b.getStatus()))
                .mapToDouble(Booking::getTotalFare)
                .sum();
        response.setTotalFareVolume(Math.round(totalFare * 100.0) / 100.0);

        // Overall vehicle seat utilization & occupancy rate
        int totalSeatsOffered = rides.stream().mapToInt(Ride::getTotalSeats).sum();
        int totalSeatsFilled = rides.stream().mapToInt(r -> r.getTotalSeats() - r.getAvailableSeats()).sum();
        double occupancyRate = (totalSeatsOffered > 0) ? ((double) totalSeatsFilled / totalSeatsOffered) * 100.0 : 0.0;
        response.setOverallOccupancyRatePercent(Math.round(occupancyRate * 10.0) / 10.0);

        // Average user rating
        double avgRating = users.stream().mapToDouble(User::getAverageRating).average().orElse(5.0);
        response.setAveragePlatformRating(Math.round(avgRating * 10.0) / 10.0);

        // Hourly Demand Breakdown (Group by hour of departure)
        Map<String, Long> hourlyMap = new HashMap<>();
        for (int h = 6; h <= 22; h += 2) {
            String slot = String.format("%02d:00 - %02d:00", h, h + 2);
            int startHour = h;
            int endHour = h + 2;
            long count = rides.stream()
                    .filter(r -> r.getDepartureTime() != null)
                    .filter(r -> r.getDepartureTime().getHour() >= startHour && r.getDepartureTime().getHour() < endHour)
                    .count();
            hourlyMap.put(slot, count);
        }
        response.setHourlyDemandBreakdown(hourlyMap);

        // Popular Routes Origin-Destination summary
        Map<String, Long> routeCounts = rides.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getOriginTitle() + " → " + r.getDestTitle(),
                        Collectors.counting()
                ));
        response.setPopularRoutes(routeCounts);

        return response;
    }
}
