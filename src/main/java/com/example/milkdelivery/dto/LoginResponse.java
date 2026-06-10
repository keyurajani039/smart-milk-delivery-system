package com.example.milkdelivery.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;

    private String refreshToken;

    private String message;

    private UserDto user;
}