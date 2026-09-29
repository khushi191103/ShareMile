package com.sharemile.service;

import com.sharemile.dto.PassengerSummaryDTO;
import com.sharemile.dto.RideCreateRequest;
import com.sharemile.dto.RideSearchRequest;
import com.sharemile.dto.RideSearchResult;
import com.sharemile.model.Booking;
import com.sharemile.model.RecurringRule;
import com.sharemile.model.Ride;
import com.sharemile.model.User;
import com.sharemile.repository.BookingRepository;
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
    private final BookingRepository bookingRepository;

    public RideService(RideRepository rideRepository,
                       UserRepository userRepository,
                       RecurringRuleRepository recurringRuleRepository,
                       BookingRepository bookingRepository) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.recurringRuleRepository = recurringRuleRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public Ride createRide(RideCreateRequest request, String driverUsername) {
        User driver = userRepository.findByUsername(driverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        if (driver.isBlacklisted()) {
            throw new IllegalArgumentException("Access Denied: Your driver account has been blacklisted by administration. You cannot offer rides.");
        }

        if ("ROLE_PASSENGER".equalsIgnoreCase(driver.getRole()) ||
            driver.getDriverLicenseNumber() == null || driver.getDriverLicenseNumber().trim().isEmpty() ||
            driver.getVehicleModel() == null || driver.getVehicleModel().trim().isEmpty() ||
            driver.getVehicleNumber() == null || driver.getVehicleNumber().trim().isEmpty() ||
            driver.getVehicleColor() == null || driver.getVehicleColor().trim().isEmpty()) {
            throw new IllegalArgumentException("Driver Verification Required: You are registered as a Passenger or your Driving License / Car details (Name, Number, Color) are incomplete. Please complete driver verification before offering rides.");
        }

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

        String genderPref = request.getGenderPreference();
        if (genderPref == null || genderPref.trim().isEmpty()) {
            genderPref = "ANY";
        }
        ride.setGenderPreference(genderPref.toUpperCase());

        // Initialize driver's live location to the origin coordinates
        ride.setDriverCurrentLat(request.getOriginLat());
        ride.setDriverCurrentLng(request.getOriginLng());
        ride.setDriverLocationUpdatedAt(LocalDateTime.now());

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
        String filterGender = search.getGenderPreference();

        for (Ride ride : scheduledRides) {
            if (ride.getDriver() != null && ride.getDriver().isBlacklisted()) {
                continue;
            }

            if (ride.getAvailableSeats() < needed) {
                continue;
            }

            // Gender preference filtering
            if (filterGender != null && !filterGender.trim().isEmpty() && !"ALL".equalsIgnoreCase(filterGender) && !"ANY".equalsIgnoreCase(filterGender)) {
                String ridePref = ride.getGenderPreference() != null ? ride.getGenderPreference().toUpperCase() : "ANY";
                if (!"ANY".equals(ridePref) && !filterGender.equalsIgnoreCase(ridePref)) {
                    continue;
                }
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
                double driverRating = (ride.getDriver() != null && ride.getDriver().getTotalRatings() > 0)
                        ? ride.getDriver().getAverageRating()
                        : 4.8;
                double ratingScore = (driverRating / 5.0) * 100.0;

                double matchScore = MatchScoreCalculator.computeMatchScore(
                        pickupDist, dropDist, radius,
                        search.getDesiredTime(), ride.getDepartureTime(), driverRating
                );

                double carbonSaved = SpatialUtils.calculateCarbonSavingsKg(
                        ride.getEstimatedDistanceKm(),
                        ride.getTotalSeats() - ride.getAvailableSeats() + 1
                );

                RideSearchResult res = new RideSearchResult(
                        ride, pickupDist, dropDist, matchScore,
                        overlap, timeScore, ratingScore, carbonSaved
                );

                // Driver Live Location & ETA calculation
                double driverLat = (ride.getDriverCurrentLat() != 0) ? ride.getDriverCurrentLat() : ride.getOriginLat();
                double driverLng = (ride.getDriverCurrentLng() != 0) ? ride.getDriverCurrentLng() : ride.getOriginLng();
                res.setDriverCurrentLat(driverLat);
                res.setDriverCurrentLng(driverLng);

                double driverToPickupDist = SpatialUtils.haversineDistanceKm(driverLat, driverLng, search.getPickupLat(), search.getPickupLng());
                res.setDriverDistanceKm(Math.round(driverToPickupDist * 10.0) / 10.0);
                int etaMin = (int) Math.max(1, Math.round((driverToPickupDist / 30.0) * 60.0));
                res.setDriverEtaMinutes(etaMin);

                res.setGenderPreference(ride.getGenderPreference() != null ? ride.getGenderPreference() : "ANY");

                // Retrieve confirmed co-passengers for passenger comfort transparency
                List<Booking> confirmed = bookingRepository.findByRideIdAndStatus(ride.getId(), "ACCEPTED");
                List<PassengerSummaryDTO> passengerList = new ArrayList<>();
                for (Booking b : confirmed) {
                    User p = b.getPassenger();
                    String gender = (p != null && p.getGender() != null) ? p.getGender() : "Not specified";
                    String name = (p != null) ? p.getFullName() : "Anonymous Passenger";
                    passengerList.add(new PassengerSummaryDTO(
                            name,
                            gender,
                            b.getSeatsBooked(),
                            b.getPassengerNames() != null ? b.getPassengerNames() : name,
                            b.getPickupTitle(),
                            b.getDropTitle()
                    ));
                }
                res.setConfirmedPassengers(passengerList);

                results.add(res);
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

    @Transactional
    public Ride updateDriverLocation(Long rideId, double lat, double lng, String driverUsername) {
        Ride ride = getRideById(rideId);
        if (!ride.getDriver().getUsername().equals(driverUsername)) {
            throw new IllegalStateException("Only the publishing driver can update their live location!");
        }
        ride.setDriverCurrentLat(lat);
        ride.setDriverCurrentLng(lng);
        ride.setDriverLocationUpdatedAt(LocalDateTime.now());
        return rideRepository.save(ride);
    }
}
