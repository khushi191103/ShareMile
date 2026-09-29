package com.sharemile.service;

import com.sharemile.dto.RatingRequest;
import com.sharemile.model.Booking;
import com.sharemile.model.RatingFeedback;
import com.sharemile.model.User;
import com.sharemile.repository.BookingRepository;
import com.sharemile.repository.RatingFeedbackRepository;
import com.sharemile.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RatingService {

    private final RatingFeedbackRepository ratingRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public RatingService(RatingFeedbackRepository ratingRepository,
                         BookingRepository bookingRepository,
                         UserRepository userRepository) {
        this.ratingRepository = ratingRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RatingFeedback submitRating(RatingRequest request, String reviewerUsername) {
        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found"));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking record not found: " + request.getBookingId()));

        User reviewee = userRepository.findById(request.getRevieweeId())
                .orElseThrow(() -> new IllegalArgumentException("Reviewee not found: " + request.getRevieweeId()));

        if (reviewer.getId().equals(reviewee.getId())) {
            throw new IllegalArgumentException("Users cannot submit reviews for themselves!");
        }

        int score = Math.max(1, Math.min(5, request.getScore()));

        RatingFeedback feedback = new RatingFeedback();
        feedback.setBooking(booking);
        feedback.setReviewer(reviewer);
        feedback.setReviewee(reviewee);
        feedback.setScore(score);
        feedback.setComment(request.getComment());

        RatingFeedback saved = ratingRepository.save(feedback);

        // Dynamically recalculate reviewee's average rating
        int previousRatingsCount = reviewee.getTotalRatings();
        double newAverage;
        if (previousRatingsCount <= 0) {
            newAverage = score;
        } else {
            double currentAverage = reviewee.getAverageRating();
            newAverage = ((currentAverage * previousRatingsCount) + score) / (previousRatingsCount + 1.0);
        }

        reviewee.setTotalRatings(previousRatingsCount + 1);
        reviewee.setAverageRating(Math.round(newAverage * 10.0) / 10.0);
        userRepository.save(reviewee);

        return saved;
    }

    public List<RatingFeedback> getUserRatings(Long userId) {
        return ratingRepository.findByRevieweeId(userId);
    }
}
