package com.sharemile.service;

import com.sharemile.dto.RideCreateRequest;
import com.sharemile.dto.RideSearchRequest;
import com.sharemile.dto.RideSearchResult;
import com.sharemile.model.RecurringRule;
import com.sharemile.model.Ride;
import com.sharemile.model.User;
import com.sharemile.repository.RecurringRuleRepository;
import com.sharemile.repository.RideRepository;
import com.sharemile.repository.UserRepository;
import com.sharemile.util.MatchScoreCalculator;
import com.sharemile.util.SpatialUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final RecurringRuleRepository recurringRuleRepository;

    public RideService(RideRepository rideRepository,
                       UserRepository userRepository,
                       RecurringRuleRepository recurringRuleRepository) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.recurringRuleRepository = recurringRuleRepository;
    }

    @Transactional
    public Ride createRide(RideCreateRequest request, String driverUsername) {
        User driver = userRepository.findByUsername(driverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setOriginTitle(request.getOriginTitle());
        ride.setOriginLat(request.getOriginLat());
        ride.setOriginLng(request.getOriginLng());
        ride.setDestTitle(request.getDestTitle());
        ride.setDestLat(request.getDestLat());
        ride.setDestLng(request.getDestLng());
        ride.setDepartureTime(request.getDepartureTime() != null ? request.getDepartureTime() : LocalDateTime.now().plusHours(2));
        ride.setTotalSeats(request.getTotalSeats());
        ride.setAvailableSeats(request.getTotalSeats());
        ride.setPricePerSeat(request.getPricePerSeat());
        ride.setStatus("SCHEDULED");

        double distance = SpatialUtils.haversineDistanceKm(
                request.getOriginLat(), request.getOriginLng(),
                request.getDestLat(), request.getDestLng()
        );
        ride.setEstimatedDistanceKm(distance);
        ride.setEstimatedDurationMin((int) Math.max(15, (distance / 30.0) * 60)); // Assumes ~30 km/h avg urban speed

        if (request.getRecurringRuleId() != null) {
            RecurringRule rule = recurringRuleRepository.findById(request.getRecurringRuleId()).orElse(null);
            ride.setRecurringRule(rule);
        }

        return rideRepository.save(ride);
    }

    public List<RideSearchResult> searchRides(RideSearchRequest search) {
        List<Ride> scheduledRides = rideRepository.findByStatus("SCHEDULED");
        List<RideSearchResult> results = new ArrayList<>();
        double radius = (search.getRadiusKm() > 0) ? search.getRadiusKm() : 10.0;
        int needed = (search.getSeatsNeeded() > 0) ? search.getSeatsNeeded() : 1;

        for (Ride ride : scheduledRides) {
            if (ride.getAvailableSeats() < needed) {
                continue;
            }

            double pickupDist = SpatialUtils.haversineDistanceKm(
                    search.getPickupLat(), search.getPickupLng(),
                    ride.getOriginLat(), ride.getOriginLng()
            );

            double dropDist = SpatialUtils.haversineDistanceKm(
                    search.getDropLat(), search.getDropLng(),
                    ride.getDestLat(), ride.getDestLng()
            );

            // Filter rides within user's geographic search radius
            if (pickupDist <= radius && dropDist <= radius) {
                double totalDist = pickupDist + dropDist;
                double overlap = Math.max(0.0, 100.0 * (1.0 - (totalDist / (radius * 2.0))));
                double timeScore = 100.0;
                if (search.getDesiredTime() != null && ride.getDepartureTime() != null) {
                    long diffMin = Math.abs(java.time.Duration.between(search.getDesiredTime(), ride.getDepartureTime()).toMinutes());
                    timeScore = (diffMin <= 15) ? 100.0 : (diffMin <= 60) ? 80.0 : Math.max(20.0, 100.0 - (diffMin / 3.0));
                }
                double driverRating = (ride.getDriver() != null) ? ride.getDriver().getAverageRating() : 5.0;
                double ratingScore = (driverRating / 5.0) * 100.0;

                double matchScore = MatchScoreCalculator.computeMatchScore(
                        pickupDist, dropDist, radius,
                        search.getDesiredTime(), ride.getDepartureTime(), driverRating
                );

                double carbonSaved = SpatialUtils.calculateCarbonSavingsKg(
                        ride.getEstimatedDistanceKm(),
                        ride.getTotalSeats() - ride.getAvailableSeats() + 1
                );

                results.add(new RideSearchResult(
                        ride, pickupDist, dropDist, matchScore,
                        overlap, timeScore, ratingScore, carbonSaved
                ));
            }
        }

        // Rank search results by Match Score descending (highest compatibility first)
        results.sort(Comparator.comparingDouble(RideSearchResult::getMatchScore).reversed());
        return results;
    }

    public Ride getRideById(Long id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found with ID: " + id));
    }

    public List<Ride> getDriverRides(String driverUsername) {
        User driver = userRepository.findByUsername(driverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        return rideRepository.findByDriverIdOrderByDepartureTimeDesc(driver.getId());
    }

    @Transactional
    public Ride updateRideStatus(Long rideId, String status, String driverUsername) {
        Ride ride = getRideById(rideId);
        if (!ride.getDriver().getUsername().equals(driverUsername)) {
            throw new IllegalStateException("Only the publishing driver can update this ride's status!");
        }
        ride.setStatus(status.toUpperCase());
        return rideRepository.save(ride);
    }
}
