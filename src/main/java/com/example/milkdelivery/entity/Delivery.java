package com.example.milkdelivery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Customer
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // Milkman
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Delivery Date
    private LocalDate deliveryDate;

    // Delivery Time
    private LocalDateTime deliveryTime;

    // Regular Milk
    private Double milkQuantity;

    // Extra Milk
    private Double extraMilk;

    // Total Milk
    private Double totalMilk;

    // DELIVERED / SKIPPED / PAUSED
    private String deliveryStatus;

    private Double latitude;

    private Double longitude;

    @CreationTimestamp
    private LocalDateTime createdAt;
}