package com.sharemile.controller;

import com.sharemile.dto.RideCreateRequest;
import com.sharemile.dto.RideSearchRequest;
import com.sharemile.dto.RideSearchResult;
import com.sharemile.model.Ride;
import com.sharemile.service.RideService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    public ResponseEntity<Ride> createRide(@RequestBody RideCreateRequest request, Authentication authentication) {
        Ride created = rideService.createRide(request, authentication.getName());
        return ResponseEntity.ok(created);
    }

    @PostMapping("/search")
    public ResponseEntity<List<RideSearchResult>> searchRides(@RequestBody RideSearchRequest request) {
        return ResponseEntity.ok(rideService.searchRides(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ride> getRideById(@PathVariable Long id) {
        return ResponseEntity.ok(rideService.getRideById(id));
    }

    @GetMapping("/my-published")
    public ResponseEntity<List<Ride>> getMyPublishedRides(Authentication authentication) {
        return ResponseEntity.ok(rideService.getDriverRides(authentication.getName()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Ride> updateStatus(@PathVariable Long id,
                                             @RequestBody Map<String, String> payload,
                                             Authentication authentication) {
        String status = payload.get("status");
        return ResponseEntity.ok(rideService.updateRideStatus(id, status, authentication.getName()));
    }
}
