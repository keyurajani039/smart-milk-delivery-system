package com.example.milkdelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryDto {

    private Long id;

    private Long customerId;

    private String customerName;

    private Long userId;

    private String milkmanName;

    private LocalDate deliveryDate;

    private LocalDateTime deliveryTime;

    private Double milkQuantity;

    private Double extraMilk;

    private Double totalMilk;

    private String deliveryStatus;

    private LocalDateTime createdAt;
}