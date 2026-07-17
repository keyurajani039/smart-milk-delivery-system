package com.example.milkdelivery.controller;

import com.example.milkdelivery.dto.FirebaseLoginRequest;
import com.example.milkdelivery.dto.ChangePasswordRequest;
import com.example.milkdelivery.dto.ForgotPasswordRequest;
import com.example.milkdelivery.dto.LoginRequest;
import com.example.milkdelivery.dto.LoginResponse;
import com.example.milkdelivery.dto.RegisterRequest;
import com.example.milkdelivery.entity.User;
import com.example.milkdelivery.service.UserService;
import com.example.milkdelivery.service.DummyDataService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private DummyDataService dummyDataService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest request) {
        User registeredUser = userService.register(request);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestBody Map<String, String> body) {
        String phoneNumber = body.get("phoneNumber");
        if (phoneNumber == null || phoneNumber.length() != 10) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid phone number. Must be 10 digits."));
        }
        try {
            String otp = userService.generateAndSendOtp(phoneNumber);
            return ResponseEntity.ok(Map.of(
                "message", "OTP sent successfully",
                "otp", otp
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(@RequestBody Map<String, String> body) {
        String token = body.get("refreshToken");
        if (token == null) {
            throw new RuntimeException("Refresh token is required");
        }
        LoginResponse response = userService.refreshToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequest request) {
        
        String response = userService.changePassword(
                userDetails.getUsername(),
                request.getOldPassword(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String response = userService.forgotPassword(request.getPhoneNumber(), request.getNewPassword());
        return ResponseEntity.ok(response);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/reset-db")
    public ResponseEntity<String> resetDb(@RequestParam(value = "seed", defaultValue = "false") boolean seed) {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0;");
        jdbcTemplate.execute("TRUNCATE TABLE deliveries;");
        jdbcTemplate.execute("TRUNCATE TABLE payments;");
        jdbcTemplate.execute("TRUNCATE TABLE trackings;");
        jdbcTemplate.execute("TRUNCATE TABLE extra_customer_sales;");
        jdbcTemplate.execute("TRUNCATE TABLE delivery_sessions;");
        jdbcTemplate.execute("TRUNCATE TABLE customers;");
        jdbcTemplate.execute("TRUNCATE TABLE users;");
        jdbcTemplate.execute("TRUNCATE TABLE milk_categories;");
        jdbcTemplate.execute("TRUNCATE TABLE plan_cancellations;");
        jdbcTemplate.execute("TRUNCATE TABLE subscription_payments;");
        jdbcTemplate.execute("TRUNCATE TABLE routes;");
        jdbcTemplate.execute("TRUNCATE TABLE route_details;");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1;");
        
        if (seed) {
            dummyDataService.seedData();
            return ResponseEntity.ok("DB Reset and Seeded Successfully");
        }
        return ResponseEntity.ok("DB Reset Successful");
    }

    @PostMapping("/seed")
    public ResponseEntity<String> seedDb() {
        dummyDataService.seedData();
        return ResponseEntity.ok("DB Seeded Successfully");
    }

    @PostMapping("/firebase-login")
    public ResponseEntity<LoginResponse> firebaseLogin(@Valid @RequestBody FirebaseLoginRequest request) {
        LoginResponse response = userService.loginWithFirebase(request);
        return ResponseEntity.ok(response);
    }
}
