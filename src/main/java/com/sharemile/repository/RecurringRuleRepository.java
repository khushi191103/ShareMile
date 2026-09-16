package com.sharemile.repository;

import com.sharemile.model.RecurringRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecurringRuleRepository extends JpaRepository<RecurringRule, Long> {
    List<RecurringRule> findByDriverId(Long driverId);
    List<RecurringRule> findByActiveTrue();
}
