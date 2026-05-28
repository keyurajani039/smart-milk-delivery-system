package com.example.milkdelivery.service;

import com.example.milkdelivery.dto.LoginRequest;
import com.example.milkdelivery.dto.LoginResponse;
import com.example.milkdelivery.entity.User;

public interface UserService {

    User saveUser(User user);

    LoginResponse login(LoginRequest request);
}