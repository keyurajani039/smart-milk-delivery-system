package com.example.milkdelivery.controller;

import com.example.milkdelivery.dto.LoginRequest;
import com.example.milkdelivery.dto.LoginResponse;
import com.example.milkdelivery.entity.User;
import com.example.milkdelivery.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/save")
    public User saveUser(@Valid @RequestBody User user) {
        return userService.saveUser(user);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody LoginRequest request) {

        return userService.login(request);
    }

    @GetMapping("/profile")
    public String profile() {

        return "JWT Protected Profile API Working";
    }
}