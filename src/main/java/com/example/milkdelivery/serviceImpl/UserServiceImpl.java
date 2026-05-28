package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.dto.LoginRequest;
import com.example.milkdelivery.dto.LoginResponse;
import com.example.milkdelivery.entity.User;
import com.example.milkdelivery.repository.UserRepository;
import com.example.milkdelivery.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.milkdelivery.dto.LoginRequest;
import com.example.milkdelivery.dto.LoginResponse;
import com.example.milkdelivery.jwt.JwtUtil;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);

    }

    @Override
    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        String token =
                jwtUtil.generateToken(user.getPhoneNumber());

        return new LoginResponse(
                token,
                "Login successful"
        );
    }
}