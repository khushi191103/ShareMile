package com.sharemile.controller;

import com.sharemile.dto.AnalyticsResponse;
import com.sharemile.dto.ComplaintRequest;
import com.sharemile.model.ReportComplaint;
import com.sharemile.model.User;
import com.sharemile.repository.ReportComplaintRepository;
import com.sharemile.repository.UserRepository;
import com.sharemile.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;
    private final ReportComplaintRepository complaintRepository;

    public AdminController(AnalyticsService analyticsService,
                           UserRepository userRepository,
                           ReportComplaintRepository complaintRepository) {
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
        this.complaintRepository = complaintRepository;
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER', 'PASSENGER')") // Permit viewing system stats
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(analyticsService.getPlatformAnalytics());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PutMapping("/users/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> toggleVerifyUser(@PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean verified = payload.getOrDefault("verified", true);
        user.setVerified(verified);
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PostMapping("/complaints")
    public ResponseEntity<ReportComplaint> submitComplaint(@RequestBody ComplaintRequest request, Authentication authentication) {
        User reporter = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Reporter not found"));
        User reported = userRepository.findById(request.getReportedUserId())
                .orElseThrow(() -> new IllegalArgumentException("Reported user not found"));

        ReportComplaint complaint = new ReportComplaint();
        complaint.setReporter(reporter);
        complaint.setReportedUser(reported);
        complaint.setReason(request.getReason());
        complaint.setDescription(request.getDescription());
        complaint.setStatus("PENDING");

        return ResponseEntity.ok(complaintRepository.save(complaint));
    }

    @GetMapping("/complaints")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReportComplaint>> getComplaints() {
        return ResponseEntity.ok(complaintRepository.findAllByOrderByCreatedAtDesc());
    }

    @PutMapping("/complaints/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportComplaint> updateComplaintStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        ReportComplaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        complaint.setStatus(payload.getOrDefault("status", "RESOLVED"));
        return ResponseEntity.ok(complaintRepository.save(complaint));
    }
}
