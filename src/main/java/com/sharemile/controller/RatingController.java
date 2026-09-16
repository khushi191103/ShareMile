package com.sharemile.controller;

import com.sharemile.dto.RatingRequest;
import com.sharemile.model.RatingFeedback;
import com.sharemile.service.RatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    public ResponseEntity<RatingFeedback> submitRating(@RequestBody RatingRequest request, Authentication authentication) {
        return ResponseEntity.ok(ratingService.submitRating(request, authentication.getName()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RatingFeedback>> getUserRatings(@PathVariable Long userId) {
        return ResponseEntity.ok(ratingService.getUserRatings(userId));
    }
}
