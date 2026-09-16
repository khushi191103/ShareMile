package com.sharemile;

import com.sharemile.dto.AuthRequest;
import com.sharemile.dto.AuthResponse;
import com.sharemile.dto.RegisterRequest;
import com.sharemile.repository.UserRepository;
import com.sharemile.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should successfully register a new user and generate JWT token")
    void testUserRegistrationAndLogin() {
        String testUser = "auth_unit_test_" + System.currentTimeMillis();

        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername(testUser);
        regReq.setEmail(testUser + "@test.com");
        regReq.setPassword("secret123");
        regReq.setFullName("Unit Tester");
        regReq.setRole("ROLE_PASSENGER");

        AuthResponse regResp = authService.register(regReq);
        assertNotNull(regResp.getToken(), "JWT token should be generated upon registration");
        assertEquals(testUser, regResp.getUsername());

        // Test duplicate registration rejection
        assertThrows(IllegalArgumentException.class, () -> authService.register(regReq));

        // Test login
        AuthRequest loginReq = new AuthRequest(testUser, "secret123");
        AuthResponse loginResp = authService.login(loginReq);
        assertNotNull(loginResp.getToken(), "JWT token should be returned upon successful login");
        assertEquals("ROLE_PASSENGER", loginResp.getRole());
    }
}
