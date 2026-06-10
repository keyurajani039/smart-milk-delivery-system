package com.example.milkdelivery.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {
    private String phoneNumber;
    private String newPassword;
}
