package com.sharemile.service;

import com.sharemile.dto.AuthRequest;
import com.sharemile.dto.AuthResponse;
import com.sharemile.dto.RegisterRequest;
import com.sharemile.model.User;
import com.sharemile.repository.UserRepository;
import com.sharemile.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered!");
        }

        boolean isDriver = "DRIVER".equalsIgnoreCase(request.getUserType()) || "ROLE_DRIVER".equalsIgnoreCase(request.getRole());
        String role = isDriver ? "ROLE_DRIVER" : "ROLE_PASSENGER";

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhone(),
                role
        );

        if (isDriver) {
            if (request.getDriverLicenseNumber() == null || request.getDriverLicenseNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("Driver License Number is mandatory for Driver Partner registration!");
            }
            if (request.getVehicleModel() == null || request.getVehicleModel().trim().isEmpty()) {
                throw new IllegalArgumentException("Car Name / Model is mandatory for Driver Partner registration!");
            }
            if (request.getVehicleNumber() == null || request.getVehicleNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("Car Registration Number is mandatory for Driver Partner registration!");
            }
            if (request.getVehicleColor() == null || request.getVehicleColor().trim().isEmpty()) {
                throw new IllegalArgumentException("Car Color is mandatory for Driver Partner registration!");
            }
            user.setDriverLicenseNumber(request.getDriverLicenseNumber().trim());
            user.setVehicleModel(request.getVehicleModel().trim());
            user.setVehicleNumber(request.getVehicleNumber().trim().toUpperCase());
            user.setVehicleColor(request.getVehicleColor().trim());
            user.setVerified(true);
        } else {
            user.setVerified(false);
            user.setDriverLicenseNumber(null);
            user.setVehicleModel(null);
            user.setVehicleNumber(null);
            user.setVehicleColor(null);
        }

        if (request.getGender() != null && !request.getGender().isBlank()) {
            user.setGender(request.getGender().trim().toUpperCase());
        } else {
            user.setGender("OTHER");
        }

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser.getUsername(), savedUser.getRole());

        AuthResponse resp = new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole(),
                savedUser.isVerified(),
                savedUser.getAverageRating()
        );
        resp.setPhone(savedUser.getPhone());
        resp.setGender(savedUser.getGender());
        resp.setDriverLicenseNumber(savedUser.getDriverLicenseNumber());
        resp.setVehicleModel(savedUser.getVehicleModel());
        resp.setVehicleNumber(savedUser.getVehicleNumber());
        resp.setVehicleColor(savedUser.getVehicleColor());
        resp.setTotalRatings(savedUser.getTotalRatings());
        resp.setBlacklisted(savedUser.isBlacklisted());
        return resp;
    }

    public AuthResponse verifyDriver(String username, com.sharemile.dto.DriverVerificationRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        if (user.isBlacklisted()) {
            throw new IllegalArgumentException("Access Denied: Your account has been blacklisted by administration.");
        }

        if (request.getDriverLicenseNumber() == null || request.getDriverLicenseNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Driving License Number is required for verification!");
        }
        if (request.getVehicleModel() == null || request.getVehicleModel().trim().isEmpty()) {
            throw new IllegalArgumentException("Car Name / Model is required for verification!");
        }
        if (request.getVehicleNumber() == null || request.getVehicleNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Car Registration Number is required for verification!");
        }
        if (request.getVehicleColor() == null || request.getVehicleColor().trim().isEmpty()) {
            throw new IllegalArgumentException("Car Color is required for verification!");
        }

        user.setRole("ROLE_DRIVER");
        user.setDriverLicenseNumber(request.getDriverLicenseNumber().trim());
        user.setVehicleModel(request.getVehicleModel().trim());
        user.setVehicleNumber(request.getVehicleNumber().trim().toUpperCase());
        user.setVehicleColor(request.getVehicleColor().trim());
        user.setVerified(true);

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser.getUsername(), savedUser.getRole());

        AuthResponse resp = new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole(),
                savedUser.isVerified(),
                savedUser.getAverageRating()
        );
        resp.setPhone(savedUser.getPhone());
        resp.setGender(savedUser.getGender());
        resp.setDriverLicenseNumber(savedUser.getDriverLicenseNumber());
        resp.setVehicleModel(savedUser.getVehicleModel());
        resp.setVehicleNumber(savedUser.getVehicleNumber());
        resp.setVehicleColor(savedUser.getVehicleColor());
        resp.setTotalRatings(savedUser.getTotalRatings());
        resp.setBlacklisted(savedUser.isBlacklisted());
        return resp;
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isBlacklisted()) {
            throw new IllegalArgumentException("Access Denied: Your account has been blacklisted by administration due to safety or policy violations.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole());

        AuthResponse resp = new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isVerified(),
                user.getAverageRating()
        );
        resp.setPhone(user.getPhone());
        resp.setGender(user.getGender());
        resp.setDriverLicenseNumber(user.getDriverLicenseNumber());
        resp.setVehicleModel(user.getVehicleModel());
        resp.setVehicleNumber(user.getVehicleNumber());
        resp.setVehicleColor(user.getVehicleColor());
        resp.setTotalRatings(user.getTotalRatings());
        resp.setBlacklisted(user.isBlacklisted());
        return resp;
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
