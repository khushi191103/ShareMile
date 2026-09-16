package com.sharemile;

import com.sharemile.util.MatchScoreCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MatchScoreCalculatorTest {

    @Test
    @DisplayName("Match score should yield 100% for perfect route overlap, immediate departure, and 5.0 driver rating")
    void testPerfectMatchScore() {
        LocalDateTime time = LocalDateTime.now().plusHours(2);
        double score = MatchScoreCalculator.computeMatchScore(
                0.0,  // pickup exactly at driver origin
                0.0,  // dropoff exactly at driver destination
                10.0, // search radius
                time, // desired time
                time, // scheduled time
                5.0   // 5-star driver
        );

        assertEquals(100.0, score, 0.5, "Perfect alignment should achieve 100% match score");
    }

    @Test
    @DisplayName("Match score should weight route overlap 50%, time proximity 30%, and driver rating 20%")
    void testWeightedMatchScore() {
        LocalDateTime desiredTime = LocalDateTime.of(2026, 9, 20, 10, 0);
        LocalDateTime rideTime = LocalDateTime.of(2026, 9, 20, 10, 10); // 10 min difference => 100% time score

        // Pickup is 2.5 km away, Dropoff is 2.5 km away, radius is 10 km (max acceptable = 20 km)
        // totalDist = 5.0, routeOverlap = 100 * (1 - 5/20) = 75%
        // timeScore = 100%
        // ratingScore = (4.0 / 5.0) * 100 = 80%
        // expected = (75 * 0.5) + (100 * 0.3) + (80 * 0.2) = 37.5 + 30.0 + 16.0 = 83.5%
        double score = MatchScoreCalculator.computeMatchScore(
                2.5, 2.5, 10.0,
                desiredTime, rideTime, 4.0
        );

        assertEquals(83.5, score, 0.5);
    }

    @Test
    @DisplayName("Match score should be bounded between 0 and 100")
    void testScoreBoundaries() {
        double score = MatchScoreCalculator.computeMatchScore(
                50.0, 50.0, 5.0,
                LocalDateTime.now(), LocalDateTime.now().plusDays(5), 1.0
        );
        assertTrue(score >= 0.0 && score <= 100.0, "Score must remain within [0, 100]");
    }
}
