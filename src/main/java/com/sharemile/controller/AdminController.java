package com.sharemile.controller;

import com.sharemile.dto.AnalyticsResponse;
import com.sharemile.dto.ComplaintRequest;
import com.sharemile.model.ReportComplaint;
import com.sharemile.model.Ride;
import com.sharemile.model.User;
import com.sharemile.repository.ReportComplaintRepository;
import com.sharemile.repository.RideRepository;
import com.sharemile.repository.UserRepository;
import com.sharemile.service.AnalyticsService;
import com.sharemile.service.CommunicationService;
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
    private final RideRepository rideRepository;
    private final CommunicationService communicationService;

    public AdminController(AnalyticsService analyticsService,
                           UserRepository userRepository,
                           ReportComplaintRepository complaintRepository,
                           RideRepository rideRepository,
                           CommunicationService communicationService) {
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
        this.complaintRepository = complaintRepository;
        this.rideRepository = rideRepository;
        this.communicationService = communicationService;
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER', 'PASSENGER', 'USER')")
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
        User saved = userRepository.save(user);

        communicationService.notifyMultiChannel(
                saved,
                verified ? "🛡️ Account Verified" : "ℹ️ Verification Status Updated",
                verified ? "Your driver/commuter profile has been officially verified by administrators." : "Your verification status has been reset by administrators.",
                "VERIFICATION_UPDATE"
        );
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/users/{id}/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> toggleBlacklistUser(@PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean blacklist = payload.getOrDefault("blacklist", true);
        user.setBlacklisted(blacklist);
        User saved = userRepository.save(user);

        if (blacklist) {
            // Cancel all open/scheduled rides for this driver
            List<Ride> rides = rideRepository.findByDriverIdOrderByDepartureTimeDesc(user.getId());
            for (Ride r : rides) {
                if ("SCHEDULED".equals(r.getStatus()) || "OPEN".equals(r.getStatus())) {
                    r.setStatus("CANCELLED");
                    rideRepository.save(r);
                }
            }

            communicationService.notifyMultiChannel(
                    user,
                    "⛔ Account Blacklisted & Suspended",
                    "Your ShareMile account has been suspended by administration following compliance or safety review. All active carpools have been cancelled.",
                    "ACCOUNT_SUSPENDED"
            );
        } else {
            communicationService.notifyMultiChannel(
                    user,
                    "✅ Account Restored",
                    "Your ShareMile account privileges have been restored by administrators.",
                    "ACCOUNT_RESTORED"
            );
        }

        return ResponseEntity.ok(saved);
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

        ReportComplaint saved = complaintRepository.save(complaint);

        // Dispatched in real time to all ADMINS via WebSocket STOMP, Email, and SMS
        List<User> admins = userRepository.findByRole("ROLE_ADMIN");
        for (User admin : admins) {
            communicationService.notifyMultiChannel(
                    admin,
                    "🚨 URGENT: Passenger Safety Complaint #" + saved.getId(),
                    "Passenger " + reporter.getFullName() + " reported Driver " + reported.getFullName() +
                    " (" + (reported.getVehicleModel() != null ? reported.getVehicleModel() : "Vehicle") +
                    ") for: [" + request.getReason() + "]. Details: " + request.getDescription(),
                    "ADMIN_COMPLAINT_ALERT"
            );
        }

        // Notify passenger that complaint is recorded
        communicationService.notifyMultiChannel(
                reporter,
                "🛡️ Safety Incident Recorded",
                "Your incident report against " + reported.getFullName() + " has been flagged to the Administration Team for immediate review.",
                "COMPLAINT_RECEIVED"
        );

        return ResponseEntity.ok(saved);
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

    @PostMapping("/complaints/{id}/blacklist-driver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> blacklistReportedDriver(@PathVariable Long id) {
        ReportComplaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found"));
        User reported = complaint.getReportedUser();
        if (reported != null) {
            reported.setBlacklisted(true);
            userRepository.save(reported);

            // Cancel any scheduled rides
            List<Ride> rides = rideRepository.findByDriverIdOrderByDepartureTimeDesc(reported.getId());
            for (Ride r : rides) {
                if ("SCHEDULED".equals(r.getStatus()) || "OPEN".equals(r.getStatus())) {
                    r.setStatus("CANCELLED");
                    rideRepository.save(r);
                }
            }

            complaint.setStatus("DRIVER_BLACKLISTED");
            complaintRepository.save(complaint);

            communicationService.notifyMultiChannel(
                    reported,
                    "⛔ Account Blacklisted & Suspended",
                    "Your driver account has been suspended by administration following passenger safety complaint #" + complaint.getId() + " (" + complaint.getReason() + ").",
                    "ACCOUNT_SUSPENDED"
            );

            communicationService.notifyMultiChannel(
                    complaint.getReporter(),
                    "🛡️ Safety Complaint Action Taken",
                    "The administration has taken disciplinary action: Driver " + reported.getFullName() + " has been blacklisted and prohibited from offering rides.",
                    "COMPLAINT_RESOLVED"
            );
        }
        return ResponseEntity.ok(Map.of("message", "Driver blacklisted successfully and complaint marked as resolved.", "status", "SUCCESS"));
    }
}
