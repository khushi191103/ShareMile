package com.sharemile;

import com.sharemile.dto.RideCreateRequest;
import com.sharemile.dto.RideSearchRequest;
import com.sharemile.dto.RideSearchResult;
import com.sharemile.model.Ride;
import com.sharemile.model.User;
import com.sharemile.repository.RideRepository;
import com.sharemile.repository.UserRepository;
import com.sharemile.service.RideService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RideServiceTest {

    @Autowired
    private RideService rideService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RideRepository rideRepository;

    @Test
    @DisplayName("Should publish ride and find it via spatial search with match score ranking")
    void testPublishAndSearchRide() {
        String driverName = "driver_spatial_" + System.currentTimeMillis();
        User driver = new User(driverName, driverName + "@test.com", "pass", "Spatial Driver", "987", "ROLE_DRIVER");
        driver.setDriverLicenseNumber("DL-1420110099999");
        driver.setVehicleModel("Honda City");
        driver.setVehicleNumber("MH-12-TEST-0001");
        driver.setVehicleColor("White");
        driver.setVerified(true);
        userRepository.save(driver);

        RideCreateRequest createReq = new RideCreateRequest();
        createReq.setOriginTitle("Hinjawadi Phase 2");
        createReq.setOriginLat(18.5913);
        createReq.setOriginLng(73.7389);
        createReq.setDestTitle("Shivajinagar");
        createReq.setDestLat(18.5314);
        createReq.setDestLng(73.8446);
        createReq.setDepartureTime(LocalDateTime.now().plusHours(3));
        createReq.setTotalSeats(3);
        createReq.setPricePerSeat(90.0);

        Ride published = rideService.createRide(createReq, driverName);
        assertNotNull(published.getId());
        assertEquals(3, published.getAvailableSeats());
        assertTrue(published.getEstimatedDistanceKm() > 10.0);

        // Search near Hinjawadi and Shivajinagar
        RideSearchRequest searchReq = new RideSearchRequest();
        searchReq.setPickupLat(18.5920); // ~100 meters from origin
        searchReq.setPickupLng(73.7395);
        searchReq.setDropLat(18.5320);   // ~100 meters from destination
        searchReq.setDropLng(73.8450);
        searchReq.setDesiredTime(LocalDateTime.now().plusHours(3));
        searchReq.setRadiusKm(5.0);
        searchReq.setSeatsNeeded(1);

        List<RideSearchResult> searchResults = rideService.searchRides(searchReq);
        assertFalse(searchResults.isEmpty(), "Published ride should be discovered in search");

        RideSearchResult topResult = searchResults.get(0);
        assertTrue(topResult.getMatchScore() > 80.0, "Close spatial & temporal match should achieve > 80% match score");
    }
}
