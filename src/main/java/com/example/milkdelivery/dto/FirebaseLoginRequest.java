package com.example.milkdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirebaseLoginRequest {

    @NotBlank(message = "Firebase ID Token is required")
    private String idToken;

    private String deviceId;
}
