package com.example.milkdelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private Long id;

    private String firstName;

    private String lastName;

    private String milkCompanyName;

    private String phoneNumber;

    private String email;

    private String telegramId;

    private String upiId;

    private String role;

    private String status;
}