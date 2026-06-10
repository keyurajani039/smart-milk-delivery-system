package com.example.milkdelivery.service;

import com.example.milkdelivery.dto.FirebaseLoginRequest;
import com.example.milkdelivery.dto.LoginRequest;
import com.example.milkdelivery.dto.LoginResponse;
import com.example.milkdelivery.dto.RegisterRequest;
import com.example.milkdelivery.entity.User;

public interface UserService {
    User saveUser(User user);
    User register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse loginWithFirebase(FirebaseLoginRequest request);
    LoginResponse refreshToken(String refreshToken);
    String changePassword(String phoneNumber, String oldPassword, String newPassword);
    String forgotPassword(String phoneNumber, String newPassword);
    User findByPhoneNumber(String phoneNumber);
}