package com.sharemile.repository;

import com.sharemile.model.RatingFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingFeedbackRepository extends JpaRepository<RatingFeedback, Long> {
    List<RatingFeedback> findByRevieweeId(Long revieweeId);
    List<RatingFeedback> findByBookingId(Long bookingId);
}
