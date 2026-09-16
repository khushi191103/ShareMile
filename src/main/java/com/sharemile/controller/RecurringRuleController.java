package com.sharemile.controller;

import com.sharemile.dto.RecurringRuleRequest;
import com.sharemile.model.RecurringRule;
import com.sharemile.model.Ride;
import com.sharemile.service.RecurringRideService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recurring")
public class RecurringRuleController {

    private final RecurringRideService recurringRideService;

    public RecurringRuleController(RecurringRideService recurringRideService) {
        this.recurringRideService = recurringRideService;
    }

    @PostMapping
    public ResponseEntity<RecurringRule> createRule(@RequestBody RecurringRuleRequest request, Authentication authentication) {
        return ResponseEntity.ok(recurringRideService.createRule(request, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<RecurringRule>> getDriverRules(Authentication authentication) {
        return ResponseEntity.ok(recurringRideService.getDriverRules(authentication.getName()));
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<List<Ride>> generateRides(@PathVariable Long id,
                                                    @RequestBody(required = false) Map<String, Integer> payload,
                                                    Authentication authentication) {
        int days = (payload != null && payload.containsKey("days")) ? payload.get("days") : 7;
        return ResponseEntity.ok(recurringRideService.generateRidesFromRule(id, days, authentication.getName()));
    }
}
