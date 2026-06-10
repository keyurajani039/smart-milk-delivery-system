package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.dto.LoginRequest;
import com.example.milkdelivery.dto.LoginResponse;
import com.example.milkdelivery.dto.RegisterRequest;
import com.example.milkdelivery.dto.UserDto;
import com.example.milkdelivery.entity.User;
import com.example.milkdelivery.entity.UserDeviceSession;
import com.example.milkdelivery.enums.AuthProvider;
import com.example.milkdelivery.enums.Role;
import com.example.milkdelivery.enums.UserStatus;
import com.example.milkdelivery.exception.ResourceNotFoundException;
import com.example.milkdelivery.jwt.JwtUtil;
import com.example.milkdelivery.repository.UserDeviceSessionRepository;
import com.example.milkdelivery.repository.UserRepository;
import com.example.milkdelivery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.milkdelivery.dto.FirebaseLoginRequest;
import com.example.milkdelivery.config.FirebaseConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeviceSessionRepository sessionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FirebaseConfig firebaseConfig;

    @Override
    public User saveUser(User user) {
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    public User register(RegisterRequest request) {
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            throw new RuntimeException("Phone number already registered");
        }

        Role userRole = Role.DELIVERY_MAN;
        if (request.getRole() != null) {
            try {
                userRole = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Keep default
            }
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .milkCompanyName(request.getMilkCompanyName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .telegramId(request.getTelegramId())
                .upiId(request.getUpiId())
                .role(userRole)
                .status(UserStatus.ACTIVE)
                .profileImage(request.getProfileImage())
                .blocked(false)
                .trialStartDate(LocalDateTime.now())
                .trialEndDate(LocalDateTime.now().plusDays(67)) // 67-day free trial!
                .subscriptionPlanId(1L) // Free Trial plan
                .authProvider(AuthProvider.LOCAL)
                .build();

        return userRepository.save(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (Boolean.TRUE.equals(user.getBlocked())) {
            throw new RuntimeException("Your account has been blocked. Contact Super Admin.");
        }

        // Session & Device Abuse Protection
        String deviceId = request.getDeviceId() != null ? request.getDeviceId() : "unknown_device";
        List<UserDeviceSession> activeSessions = sessionRepository.findByUserId(user.getId());

        boolean deviceExists = activeSessions.stream()
                .anyMatch(s -> s.getDeviceId().equals(deviceId));

        if (!deviceExists) {
            // Auto logout oldest device if limit exceeded (e.g. max 2 devices)
            if (activeSessions.size() >= 2) {
                UserDeviceSession oldest = activeSessions.get(0);
                sessionRepository.delete(oldest);
            }
            UserDeviceSession session = UserDeviceSession.builder()
                    .user(user)
                    .deviceId(deviceId)
                    .lastActive(LocalDateTime.now())
                    .build();
            sessionRepository.save(session);
        } else {
            // Update last active
            sessionRepository.findByUserIdAndDeviceId(user.getId(), deviceId).ifPresent(s -> {
                s.setLastActive(LocalDateTime.now());
                sessionRepository.save(s);
            });
        }

        String accessToken = jwtUtil.generateToken(user.getPhoneNumber());
        String refreshToken = jwtUtil.generateRefreshToken(user.getPhoneNumber());

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .milkCompanyName(user.getMilkCompanyName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .telegramId(user.getTelegramId())
                .upiId(user.getUpiId())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .build();

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .message("Login successful")
                .user(userDto)
                .build();
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String phoneNumber = jwtUtil.extractPhoneNumber(refreshToken);
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getBlocked())) {
            throw new RuntimeException("Your account is blocked.");
        }

        String newAccessToken = jwtUtil.generateToken(user.getPhoneNumber());

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .milkCompanyName(user.getMilkCompanyName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .telegramId(user.getTelegramId())
                .upiId(user.getUpiId())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .build();

        return LoginResponse.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken)
                .message("Token refreshed successfully")
                .user(userDto)
                .build();
    }

    @Override
    public String changePassword(String phoneNumber, String oldPassword, String newPassword) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Current password does not match");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return "Password changed successfully";
    }

    @Override
    public String forgotPassword(String phoneNumber, String newPassword) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return "Password reset successfully";
    }

    @Override
    public User findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with phone: " + phoneNumber));
    }

    @Override
    public LoginResponse loginWithFirebase(FirebaseLoginRequest request) {
        String phoneNumber = null;
        String idToken = request.getIdToken();

        if (idToken != null && idToken.startsWith("mock_firebase_token_")) {
            phoneNumber = idToken.substring("mock_firebase_token_".length());
            if (phoneNumber.length() > 10) {
                phoneNumber = phoneNumber.substring(phoneNumber.length() - 10);
            }
        } else {
            if (!firebaseConfig.isFirebaseInitialized()) {
                throw new RuntimeException("Firebase Admin SDK is not initialized. Real token verification is unavailable.");
            }
            try {
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
                String rawPhoneNumber = (String) decodedToken.getClaims().get("phone_number");
                if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
                    throw new RuntimeException("No phone number found in the verified Firebase token.");
                }
                phoneNumber = rawPhoneNumber.replaceAll("\\D", "");
                if (phoneNumber.length() >= 10) {
                    phoneNumber = phoneNumber.substring(phoneNumber.length() - 10);
                }
            } catch (Exception e) {
                throw new RuntimeException("Firebase token verification failed: " + e.getMessage(), e);
            }
        }

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new RuntimeException("Could not extract a valid phone number from the token.");
        }

        final String finalPhone = phoneNumber;
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not registered with phone: " + finalPhone));

        if (Boolean.TRUE.equals(user.getBlocked())) {
            throw new RuntimeException("Your account has been blocked. Contact Super Admin.");
        }

        String deviceId = request.getDeviceId() != null ? request.getDeviceId() : "unknown_device";
        List<UserDeviceSession> activeSessions = sessionRepository.findByUserId(user.getId());

        boolean deviceExists = activeSessions.stream()
                .anyMatch(s -> s.getDeviceId().equals(deviceId));

        if (!deviceExists) {
            if (activeSessions.size() >= 2) {
                UserDeviceSession oldest = activeSessions.get(0);
                sessionRepository.delete(oldest);
            }
            UserDeviceSession session = UserDeviceSession.builder()
                    .user(user)
                    .deviceId(deviceId)
                    .lastActive(LocalDateTime.now())
                    .build();
            sessionRepository.save(session);
        } else {
            sessionRepository.findByUserIdAndDeviceId(user.getId(), deviceId).ifPresent(s -> {
                s.setLastActive(LocalDateTime.now());
                sessionRepository.save(s);
            });
        }

        String accessToken = jwtUtil.generateToken(user.getPhoneNumber());
        String refreshToken = jwtUtil.generateRefreshToken(user.getPhoneNumber());

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .milkCompanyName(user.getMilkCompanyName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .telegramId(user.getTelegramId())
                .upiId(user.getUpiId())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .build();

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .message("Login successful via Firebase")
                .user(userDto)
                .build();
    }
}