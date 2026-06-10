package com.example.milkdelivery.entity;

import com.example.milkdelivery.enums.AuthProvider;
import com.example.milkdelivery.enums.Role;
import com.example.milkdelivery.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Milk company name is required")
    private String milkCompanyName;

    @Column(unique = true)
    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Phone number must be 10 digits"
    )
    private String phoneNumber;

    @Column(unique = true)
    @Email(message = "Invalid email")
    private String email;

    private String telegramId;

    private String deviceId;

    private String password;

    private String profileImage;

    private LocalDateTime trialStartDate;

    private LocalDateTime trialEndDate;

    private LocalDateTime subscriptionEndDate;

    @Column(name = "is_blocked")
    private Boolean blocked = false;

    private Long subscriptionPlanId;

    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    private String providerId;

    @Column(name = "upi_id")
    private String upiId;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @JsonManagedReference
    @OneToMany(mappedBy = "user")
    private List<Customer> customers;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}