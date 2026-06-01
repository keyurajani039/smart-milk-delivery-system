package com.example.milkdelivery.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Phone number must be 10 digits"
    )
    private String phoneNumber;

    private String address;

    private Double latitude;

    private Double longitude;

    // Permanent milk quantity
    private Double milkQuantity;

    // Extra milk quantity
    private Double extraMilk = 0.0;

    // Remaining extra milk days
    private Integer extraMilkDays = 0;

    // Pause delivery
    private Boolean isPaused = false;

    // Remaining pause days
    private Integer pauseDays = 0;

    // Pause start date
    private LocalDate pauseStartDate;

    // Pause end date
    private LocalDate pauseEndDate;

    // Active customer
    private Boolean active = true;

    // Today's delivery completed or not
    private Boolean deliveryCompleted = false;

    // Milk type
    private String milkType;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}