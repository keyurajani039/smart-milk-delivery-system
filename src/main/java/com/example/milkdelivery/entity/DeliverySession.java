package com.example.milkdelivery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliverySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Milkman
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Delivery start time
    private LocalDateTime startedAt;

    // Delivery end time
    private LocalDateTime endedAt;

    // Session active or not
    private Boolean active = true;

    private Double loadedMilkQuantity = 0.0;

    @CreationTimestamp
    private LocalDateTime createdAt;
}