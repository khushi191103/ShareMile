package com.sharemile.repository;

import com.sharemile.model.ReportComplaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportComplaintRepository extends JpaRepository<ReportComplaint, Long> {
    List<ReportComplaint> findAllByOrderByCreatedAtDesc();
    List<ReportComplaint> findByStatus(String status);
}
